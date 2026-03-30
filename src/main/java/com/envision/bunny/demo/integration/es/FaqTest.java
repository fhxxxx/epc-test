package com.envision.bunny.demo.integration.es;

import com.envision.bunny.demo.integration.es.application.ElasticSearchRemote;
import com.envision.bunny.demo.integration.es.domain.Faq;
import com.envision.bunny.demo.integration.es.domain.FaqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * @author jingjing.dong
 * @since 2024/1/14-10:12
 */
@RestController
@RequestMapping("/faq")
public class FaqTest {
    @Autowired
    FaqService faqService;

    @Autowired
    ElasticSearchRemote remote;
    @PostMapping("/init")
    public void insertByPost() {
        final List<Faq> faqList = faqService.list();
        faqList.forEach(x -> remote.putDoc(x));
    }

    @PostMapping("/search")
    public List<Faq> search(String query) {
        final double maxScore = remote.getMaxScore(query);
        if (maxScore == 0) {
            return Collections.EMPTY_LIST;
        }
        return remote.search(query, maxScore * 0.7);
    }
}
