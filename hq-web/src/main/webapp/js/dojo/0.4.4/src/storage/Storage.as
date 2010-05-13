/*
	Copyright (c) 2004-2006, The Dojo Foundation
	All Rights Reserved.

	Licensed under the Academic Free License version 2.1 or above OR the
	modified BSD license. For more information on Dojo licensing, see:

		http://dojotoolkit.org/community/licensing.shtml
*/

import DojoExternalInterface;

class Storage {
	public static var SUCCESS = "success";
	public static var FAILED = "failed";
	public static var PENDING = "pending";

	//	Wait the following number of milliseconds before flushing
	public static var FLUSH_DELAY_DEFAULT = 500;
	
	public var flush_delay;
	public var so;
	public var timer;
	
	public function Storage(){
		flush_delay = Storage.FLUSH_DELAY_DEFAULT;
	
		//getURL("javascript:dojo.debug('FLASH:Storage constructor')");
		DojoExternalInterface.initialize();
		DojoExternalInterface.addCallback("put", this, put);
		DojoExternalInterface.addCallback("get", this, get);
		DojoExternalInterface.addCallback("showSettings", this, showSettings);
		DojoExternalInterface.addCallback("clear", this, clear);
		DojoExternalInterface.addCallback("getKeys", this, getKeys);
		DojoExternalInterface.addCallback("remove", this, remove);
		DojoExternalInterface.addCallback("flush", this, flush);
		DojoExternalInterface.addCallback("setFlushDelay", this, setFlushDelay);
		DojoExternalInterface.addCallback("getFlushDelay", this, getFlushDelay);
		DojoExternalInterface.loaded();
		
		// preload the System Settings finished button movie for offline
		// access so it is in the cache
		_root.createEmptyMovieClip("_settingsBackground", 1);
		// getURL("javascript:alert('"+DojoExternalInterface.dojoPath+"');");
		_root._settingsBackground.loadMovie(DojoExternalInterface.dojoPath + "storage_dialog.swf");
	}
	
	//	Set a new value for the flush delay timer.
	//	Possible values:
	//	  0 : Perform the flush synchronously after each "put" request
	//	> 0 : Wait until 'newDelay' ms have passed without any "put" request to flush
	//	 -1 : Do not  automatically flush
	public function setFlushDelay(newDelay){
		flush_delay = Number(newDelay);
	}
	public function getFlushDelay(){
		return String(flush_delay);
	}
	
	public function put(keyName, keyValue, namespace){
		// Get the SharedObject for these values and save it
		so = SharedObject.getLocal(namespace);
		
		// prepare a storage status handler
		var self = this;
		so.onStatus = function(infoObject:Object){
			//getURL("javascript:dojo.debug('FLASH: onStatus, infoObject="+infoObject.code+"')");
			
			// delete the data value if the request was denied
			if (infoObject.code == "SharedObject.Flush.Failed"){
				delete self.so.data[keyName];
			}
			
			var statusResults;
			if(infoObject.code == "SharedObject.Flush.Failed"){
				statusResults = Storage.FAILED;
			}else if(infoObject.code == "SharedObject.Flush.Pending"){
				statusResults = Storage.PENDING;
			}else if(infoObject.code == "SharedObject.Flush.Success"){
				statusResults = Storage.SUCCESS;
			}
			//getURL("javascript:dojo.debug('FLASH: onStatus, statusResults="+statusResults+"')");
			
			// give the status results to JavaScript
			DojoExternalInterface.call("dojo.storage._onStatus", null, statusResults, keyName);
		}
		
		//getURL("javascript:dojo.debug('FLASH: before put')");

		//  Save the key and value
		so.data[keyName] = keyValue;

		//	Clear any pending flush timers
		if(timer){
			//getURL("javascript:dojo.debug('FLASH: clearing timer')");
			_global.clearTimeout(timer);
		}
		
		//getURL("javascript:dojo.debug('FLASH: flush_delay = [" + flush_delay + "]')");
		
		//	If we have a flush delay set, set a timer for its execution
		if(flush_delay > 0){
			timer = _global.setTimeout(flush, flush_delay, namespace);
		//	With a flush_delay value of 0, execute the flush request synchronously
		}else if(flush_delay == 0){
			//getURL("javascript:dojo.debug('FLASH: calling flush now')");
			flush(namespace);
		}
		//	Otherwise just don't flush - will be probably be flushed manually
	}
	
	public function flush(namespace){
		if(timer){
			_global.clearTimeout(timer);
			delete timer;
		}
	
		var so = SharedObject.getLocal(namespace);

			//var st = (new Date()).getTime();
		var flushResults = so.flush();
			//var end = (new Date()).getTime();
			//getURL("javascript:dojo.debug('FLASH: flush - not a word game - took " + (end - st) + "ms')");

		// return results of this command to JavaScript
		var statusResults;
		if(flushResults == true){
			statusResults = Storage.SUCCESS;
		}else if(flushResults == "pending"){
			statusResults = Storage.PENDING;
		}else{
			statusResults = Storage.FAILED;
		}
		
		DojoExternalInterface.call("dojo.storage._onStatus", null, statusResults, null);
	}

	public function get(keyName, namespace){
		// Get the SharedObject for these values and save it
		so = SharedObject.getLocal(namespace);
		var results = so.data[keyName];
		
		return results;
	}
	
	public function showSettings(){
		// Show the configuration options for the Flash player, opened to the
		// section for local storage controls (pane 1)
		System.showSettings(1);
		
		// there is no way we can intercept when the Close button is pressed, allowing us
		// to hide the Flash dialog. Instead, we need to load a movie in the
		// background that we can show a close button on.
		_root.createEmptyMovieClip("_settingsBackground", 1);
		_root._settingsBackground.loadMovie(DojoExternalInterface.dojoPath + "storage_dialog.swf");
	}
	
	public function clear(namespace){
		so = SharedObject.getLocal(namespace);
		so.clear();
		so.flush();
	}
	
	public function getKeys(namespace){
		// Returns a list of the available keys in this namespace
		
		// get the storage object
		so = SharedObject.getLocal(namespace);
		
		// get all of the keys
		var results = new Array();
		for(var i in so.data)
			results.push(i);	
		
		// join the keys together in a comma seperated string
		results = results.join(",");
		
		return results;
	}
	
	public function remove(keyName, namespace){
		// Removes a key

		// get the storage object
		so = SharedObject.getLocal(namespace);
		
		// delete this value
		delete so.data[keyName];
		
		// save the changes
		so.flush();
	}

	static function main(mc){
		//getURL("javascript:dojo.debug('FLASH: storage loaded')");
		_root.app = new Storage(); 
	}
}

