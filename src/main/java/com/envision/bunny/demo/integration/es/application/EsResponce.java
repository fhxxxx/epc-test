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
public class EsResponce {

    private int took;
    private boolean timed_out;
    private _shards _shards;
    private Hits hits;
    public void setTook(int took) {
         this.took = took;
     }
     public int getTook() {
         return took;
     }

    public void setTimed_out(boolean timed_out) {
         this.timed_out = timed_out;
     }
     public boolean getTimed_out() {
         return timed_out;
     }

    public void set_shards(_shards _shards) {
         this._shards = _shards;
     }
     public _shards get_shards() {
         return _shards;
     }

    public void setHits(Hits hits) {
         this.hits = hits;
     }
     public Hits getHits() {
         return hits;
     }

}