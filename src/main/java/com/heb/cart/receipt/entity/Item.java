package com.heb.cart.receipt.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Item {
	
	private String itemName;
	private Long sku;
	private Boolean isTaxable;
	private Boolean ownBrand;
	private BigDecimal price;
	
}
