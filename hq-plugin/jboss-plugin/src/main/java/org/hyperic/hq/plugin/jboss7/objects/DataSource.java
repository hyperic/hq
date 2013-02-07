/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.plugin.jboss7.objects;

import com.google.gson.annotations.SerializedName;

public class DataSource {

    private boolean enabled;
    private String jndiName;
    private String driverName;

    /**
     * @return the enabled
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return the jndiName
     */
    public final String getJndiName() {
        return jndiName;
    }

    /**
     * @param jndiName the jndiName to set
     */
    public final void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    /**
     * @return the driverName
     */
    public final String getDriverName() {
        return driverName;
    }

    /**
     * @param driverName the driverName to set
     */
    public final void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    @SerializedName("ActiveCount")
    private String activeCount;
    @SerializedName("AvailableCount")
    private String availableCount;
    @SerializedName("AverageBlockingTime")
    private String averageBlockingTime;
    @SerializedName("AverageCreationTime")
    private String averageCreationTime;
    @SerializedName("CreatedCount")
    private String createdCount;
    @SerializedName("DestroyedCount")
    private String destroyedCount;
    @SerializedName("MaxCreationTime")
    private String maxCreationTime;
    @SerializedName("MaxUsedCount")
    private String maxUsedCount;
    @SerializedName("MaxWaitCount")
    private String maxWaitCount;
    @SerializedName("MaxWaitTime")
    private String maxWaitTime;
    @SerializedName("TimedOut")
    private String timedOut;
    @SerializedName("TotalBlockingTime")
    private String totalBlockingTime;
    @SerializedName("TotalCreationTime")
    private String totalCreationTime;

    /**
     * @return the activeCount
     */
    public String getActiveCount() {
        return activeCount;
    }

    /**
     * @param activeCount the activeCount to set
     */
    public void setActiveCount(String activeCount) {
        this.activeCount = activeCount;
    }

    /**
     * @return the availableCount
     */
    public String getAvailableCount() {
        return availableCount;
    }

    /**
     * @param availableCount the availableCount to set
     */
    public void setAvailableCount(String availableCount) {
        this.availableCount = availableCount;
    }

    /**
     * @return the averageBlockingTime
     */
    public String getAverageBlockingTime() {
        return averageBlockingTime;
    }

    /**
     * @param averageBlockingTime the averageBlockingTime to set
     */
    public void setAverageBlockingTime(String averageBlockingTime) {
        this.averageBlockingTime = averageBlockingTime;
    }

    /**
     * @return the averageCreationTime
     */
    public String getAverageCreationTime() {
        return averageCreationTime;
    }

    /**
     * @param averageCreationTime the averageCreationTime to set
     */
    public void setAverageCreationTime(String averageCreationTime) {
        this.averageCreationTime = averageCreationTime;
    }

    /**
     * @return the createdCount
     */
    public String getCreatedCount() {
        return createdCount;
    }

    /**
     * @param createdCount the createdCount to set
     */
    public void setCreatedCount(String createdCount) {
        this.createdCount = createdCount;
    }

    /**
     * @return the destroyedCount
     */
    public String getDestroyedCount() {
        return destroyedCount;
    }

    /**
     * @param destroyedCount the destroyedCount to set
     */
    public void setDestroyedCount(String destroyedCount) {
        this.destroyedCount = destroyedCount;
    }

    /**
     * @return the maxCreationTime
     */
    public String getMaxCreationTime() {
        return maxCreationTime;
    }

    /**
     * @param maxCreationTime the maxCreationTime to set
     */
    public void setMaxCreationTime(String maxCreationTime) {
        this.maxCreationTime = maxCreationTime;
    }

    /**
     * @return the maxUsedCount
     */
    public String getMaxUsedCount() {
        return maxUsedCount;
    }

    /**
     * @param maxUsedCount the maxUsedCount to set
     */
    public void setMaxUsedCount(String maxUsedCount) {
        this.maxUsedCount = maxUsedCount;
    }

    /**
     * @return the maxWaitCount
     */
    public String getMaxWaitCount() {
        return maxWaitCount;
    }

    /**
     * @param maxWaitCount the maxWaitCount to set
     */
    public void setMaxWaitCount(String maxWaitCount) {
        this.maxWaitCount = maxWaitCount;
    }

    /**
     * @return the maxWaitTime
     */
    public String getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * @param maxWaitTime the maxWaitTime to set
     */
    public void setMaxWaitTime(String maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    /**
     * @return the timedOut
     */
    public String getTimedOut() {
        return timedOut;
    }

    /**
     * @param timedOut the timedOut to set
     */
    public void setTimedOut(String timedOut) {
        this.timedOut = timedOut;
    }

    /**
     * @return the totalBlockingTime
     */
    public String getTotalBlockingTime() {
        return totalBlockingTime;
    }

    /**
     * @param totalBlockingTime the totalBlockingTime to set
     */
    public void setTotalBlockingTime(String totalBlockingTime) {
        this.totalBlockingTime = totalBlockingTime;
    }

    /**
     * @return the totalCreationTime
     */
    public String getTotalCreationTime() {
        return totalCreationTime;
    }

    /**
     * @param totalCreationTime the totalCreationTime to set
     */
    public void setTotalCreationTime(String totalCreationTime) {
        this.totalCreationTime = totalCreationTime;
    }
    @SerializedName("PreparedStatementCacheAccessCount")
    private String preparedStatementCacheAccessCount;
    @SerializedName("PreparedStatementCacheAddCount")
    private String preparedStatementCacheAddCount;
    @SerializedName("PreparedStatementCacheCurrentSize")
    private String preparedStatementCacheCurrentSize;
    @SerializedName("PreparedStatementCacheDeleteCount")
    private String preparedStatementCacheDeleteCount;
    @SerializedName("PreparedStatementCacheHitCount")
    private String preparedStatementCacheHitCount;
    @SerializedName("PreparedStatementCacheMissCount")
    private String preparedStatementCacheMissCount;

    /**
     * @return the preparedStatementCacheAccessCount
     */
    public String getPreparedStatementCacheAccessCount() {
        return preparedStatementCacheAccessCount;
    }

    /**
     * @param preparedStatementCacheAccessCount the
     * preparedStatementCacheAccessCount to set
     */
    public void setPreparedStatementCacheAccessCount(String preparedStatementCacheAccessCount) {
        this.preparedStatementCacheAccessCount = preparedStatementCacheAccessCount;
    }

    /**
     * @return the preparedStatementCacheAddCount
     */
    public String getPreparedStatementCacheAddCount() {
        return preparedStatementCacheAddCount;
    }

    /**
     * @param preparedStatementCacheAddCount the preparedStatementCacheAddCount
     * to set
     */
    public void setPreparedStatementCacheAddCount(String preparedStatementCacheAddCount) {
        this.preparedStatementCacheAddCount = preparedStatementCacheAddCount;
    }

    /**
     * @return the preparedStatementCacheCurrentSize
     */
    public String getPreparedStatementCacheCurrentSize() {
        return preparedStatementCacheCurrentSize;
    }

    /**
     * @param preparedStatementCacheCurrentSize the
     * preparedStatementCacheCurrentSize to set
     */
    public void setPreparedStatementCacheCurrentSize(String preparedStatementCacheCurrentSize) {
        this.preparedStatementCacheCurrentSize = preparedStatementCacheCurrentSize;
    }

    /**
     * @return the preparedStatementCacheDeleteCount
     */
    public String getPreparedStatementCacheDeleteCount() {
        return preparedStatementCacheDeleteCount;
    }

    /**
     * @param preparedStatementCacheDeleteCount the
     * preparedStatementCacheDeleteCount to set
     */
    public void setPreparedStatementCacheDeleteCount(String preparedStatementCacheDeleteCount) {
        this.preparedStatementCacheDeleteCount = preparedStatementCacheDeleteCount;
    }

    /**
     * @return the preparedStatementCacheHitCount
     */
    public String getPreparedStatementCacheHitCount() {
        return preparedStatementCacheHitCount;
    }

    /**
     * @param preparedStatementCacheHitCount the preparedStatementCacheHitCount
     * to set
     */
    public void setPreparedStatementCacheHitCount(String preparedStatementCacheHitCount) {
        this.preparedStatementCacheHitCount = preparedStatementCacheHitCount;
    }

    /**
     * @return the preparedStatementCacheMissCount
     */
    public String getPreparedStatementCacheMissCount() {
        return preparedStatementCacheMissCount;
    }

    /**
     * @param preparedStatementCacheMissCount the
     * preparedStatementCacheMissCount to set
     */
    public void setPreparedStatementCacheMissCount(String preparedStatementCacheMissCount) {
        this.preparedStatementCacheMissCount = preparedStatementCacheMissCount;
    }

    @Override
    public String toString() {
        return "DataSource71{" + "enabled=" + enabled + ", jndiName=" + jndiName + ", driverName=" + driverName + ", activeCount=" + activeCount + ", availableCount=" + availableCount + ", averageBlockingTime=" + averageBlockingTime + ", averageCreationTime=" + averageCreationTime + ", createdCount=" + createdCount + ", destroyedCount=" + destroyedCount + ", maxCreationTime=" + maxCreationTime + ", maxUsedCount=" + maxUsedCount + ", maxWaitCount=" + maxWaitCount + ", maxWaitTime=" + maxWaitTime + ", timedOut=" + timedOut + ", totalBlockingTime=" + totalBlockingTime + ", totalCreationTime=" + totalCreationTime + ", preparedStatementCacheAccessCount=" + preparedStatementCacheAccessCount + ", preparedStatementCacheAddCount=" + preparedStatementCacheAddCount + ", preparedStatementCacheCurrentSize=" + preparedStatementCacheCurrentSize + ", preparedStatementCacheDeleteCount=" + preparedStatementCacheDeleteCount + ", preparedStatementCacheHitCount=" + preparedStatementCacheHitCount + ", preparedStatementCacheMissCount=" + preparedStatementCacheMissCount + '}';
    }
}