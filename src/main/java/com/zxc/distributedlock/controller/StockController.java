package com.zxc.distributedlock.controller;

import com.zxc.distributedlock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xiaoye
 * @create 6/26/23 5:10 PM
 */
@Slf4j
@RestController
public class StockController {

    @Autowired
    private StockService stockService;



    @GetMapping("/stock/deduct")
    public String deduct() {
        log.info(stockService.toString());
        stockService.dedust();
        return "hello stock deduct";
    }

    @GetMapping("/stock/deductZk")
    public String deductZk() {
        stockService.dedustZk();
        return "hello stock deduct";
    }

}
