/**
  * Copyright 2024 bejson.com 
  */
package com.envision.bunny.demo.integration.es.application;

/**
 * Auto-generated: 2024-01-14 11:22:19
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class _shards {

    private int total;
    private int successful;
    private int skipped;
    private int failed;
    public void setTotal(int total) {
         this.total = total;
     }
     public int getTotal() {
         return total;
     }

    public void setSuccessful(int successful) {
         this.successful = successful;
     }
     public int getSuccessful() {
         return successful;
     }

    public void setSkipped(int skipped) {
         this.skipped = skipped;
     }
     public int getSkipped() {
         return skipped;
     }

    public void setFailed(int failed) {
         this.failed = failed;
     }
     public int getFailed() {
         return failed;
     }

}