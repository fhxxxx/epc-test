package com.envision.bunny.demo.capability.validation;

import com.envision.bunny.demo.support.model.Customer;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.concurrent.CompletableFuture;

/**
 * CRUD Data
 * @author jingjing.dong
 * @since 2021/4/26-15:31
 */
@RestController
@RequestMapping("/valid")
@Validated
public class ValidationTest {


    @PostMapping("/create")
    public long insertByPost(@Valid @RequestBody Customer customer) {
        return customer.getCustomerId();
    }



    @GetMapping("/async")
    public DeferredResult<String> async() {
        DeferredResult<String> deferredResult = new DeferredResult<>();

        // Start an asynchronous operation
        CompletableFuture.supplyAsync(() -> {
            // Simulate a long-running task
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Async result";
        }).whenCompleteAsync((result, throwable) -> {
            if (throwable != null) {
                deferredResult.setErrorResult(throwable);
            } else {
                deferredResult.setResult(result);
            }
        });

        return deferredResult;
    }

}
