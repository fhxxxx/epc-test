/**
  * Copyright 2024 bejson.com 
  */
package com.envision.bunny.demo.integration.es.application;
import java.util.List;

/**
 * Auto-generated: 2024-01-14 11:22:19
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class Hits {

    private Total total;
    private double max_score;
    private List<Sub_Hits> hits;
    public void setTotal(Total total) {
         this.total = total;
     }
     public Total getTotal() {
         return total;
     }

    public void setMax_score(double max_score) {
         this.max_score = max_score;
     }
     public double getMax_score() {
         return max_score;
     }

    public void setHits(List<Sub_Hits> hits) {
         this.hits = hits;
     }
     public List<Sub_Hits> getHits() {
         return hits;
     }

}