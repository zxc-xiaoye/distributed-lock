package com.zxc.distributedlock.entity;

import lombok.Data;
import org.springframework.stereotype.Service;

/**
 * @author xiaoye
 * @create 6/26/23 5:10 PM
 */
@Data
@Service
public class Stock {

    private Integer stock = 5000;
}
