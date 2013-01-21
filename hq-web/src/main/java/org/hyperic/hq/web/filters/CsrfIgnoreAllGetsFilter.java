package org.hyperic.hq.web.filters;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.Constants;
import org.apache.catalina.filters.CsrfPreventionFilter;

public class CsrfIgnoreAllGetsFilter extends CsrfPreventionFilter {
	private int nonceCacheSize = 5;

	/**
	 * Sets the number of previously issued nonces that will be cached on a LRU
	 * basis to support parallel requests, limited use of the refresh and back
	 * in the browser and similar behaviors that may result in the submission
	 * of a previous nonce rather than the current one. If not set, the default
	 * value of 5 will be used.
	 * 
	 * @param nonceCacheSize    The number of nonces to cache
	 */
	public void setNonceCacheSize(int nonceCacheSize) {
		if (getLogger().isDebugEnabled()) {
			getLogger().debug("Nonce cache size set at " + nonceCacheSize);
		}

		this.nonceCacheSize = nonceCacheSize;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,	FilterChain chain) throws IOException, ServletException {
		ServletResponse wResponse = null;
		boolean debug = getLogger().isDebugEnabled();

		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest req = (HttpServletRequest) request;
			HttpServletResponse res = (HttpServletResponse) response;
			boolean skipNonceCheck = Constants.METHOD_GET.equals(req.getMethod());

			if (debug && skipNonceCheck) {
				getLogger().debug("Nonce check will be skip for this request");
			}

			@SuppressWarnings("unchecked")
			LruCache<String> nonceCache = (LruCache<String>) req.getSession(true).getAttribute(Constants.CSRF_NONCE_SESSION_ATTR_NAME);

			if (!skipNonceCheck) {
				String previousNonce = req.getParameter(Constants.CSRF_NONCE_REQUEST_PARAM);

				if (debug) {
					getLogger().debug("Checking nonce [" + previousNonce + "]");
				}

				if (nonceCache != null && !nonceCache.contains(previousNonce)) {
					//clear the cache
					//Fix for Jira HQ-3716
					nonceCache.clear(); 
				}
			}

			if (nonceCache == null) {
				if (debug) {
					getLogger().debug("Create nonce cache of size [" + nonceCacheSize + "]");
				}

				nonceCache = new LruCache<String>(nonceCacheSize);

				req.getSession().setAttribute(Constants.CSRF_NONCE_SESSION_ATTR_NAME, nonceCache);
			}

			String nonce;

			if (!skipNonceCheck || nonceCache.isEmpty()) {
				// ...generate a new nonce if nonce has been checked or there are no nonces cached...
				nonce = generateNonce();

				if (debug) {
					getLogger().debug("New nonce generated [" + nonce + "]");
				}
			} else {
				// ...get the latest nonce if this is essentially a GET request and nonces exist in the cache...
				nonce = nonceCache.getLastEntry();

				if (debug) {
					getLogger().debug("Reusing existing nonce [" + nonce + "]");
				}
			}

			nonceCache.add(nonce);

			wResponse = new CsrfResponseWrapper(res, nonce);

		} else {
			wResponse = response;
		}

		chain.doFilter(request, wResponse);
	}

	private static class LruCache<T> {

		// Although the internal implementation uses a Map, this cache
		// implementation is only concerned with the keys.
		private final Map<T,T> cache;

		public LruCache(final int cacheSize) {
			cache = new LinkedHashMap<T,T>() {
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(Map.Entry<T,T> eldest) {
					if (size() > cacheSize) {
						return true;
					}
					return false;
				}
			};
		}
		
		public void clear() {
			cache.clear();
		}

		public void add(T key) {
			cache.put(key, null);
		}

		public boolean contains(T key) {
			return cache.containsKey(key);
		}

		public boolean isEmpty() {
			return cache.isEmpty();
		}

		public T getLastEntry() {
			T item = null;
			Iterator<T> i = cache.keySet().iterator();

			while (i.hasNext()) {
				item = i.next();
			}

			return item;
		}
	}
}

