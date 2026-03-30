/**
  * Copyright 2024 bejson.com 
  */
package com.envision.bunny.demo.integration.es.application;

import com.envision.bunny.demo.integration.es.domain.Faq;

/**
 * Auto-generated: 2024-01-14 11:22:19
 *
 * @author bejson.com (i@bejson.com)
 * @website http://www.bejson.com/java2pojo/
 */
public class Sub_Hits {

    private String _index;
    private String _type;
    private String _id;
    private double _score;
    private Faq _source;

    public String get_index() {
        return _index;
    }

    public void set_index(String _index) {
        this._index = _index;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public double get_score() {
        return _score;
    }

    public void set_score(double _score) {
        this._score = _score;
    }

    public Faq get_source() {
        return _source;
    }

    public void set_source(Faq _source) {
        this._source = _source;
    }
}