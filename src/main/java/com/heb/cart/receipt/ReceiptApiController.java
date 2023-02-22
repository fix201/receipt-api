package com.heb.cart.receipt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.heb.cart.receipt.entity.Coupon;
import com.heb.cart.receipt.entity.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Receipt API to calculate different totals
 * @author Joseph Ayodele
 */
@RestController
@RequestMapping("/receipt")
public class ReceiptApiController implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(ReceiptApiController.class);
	private static final String ITEMS = "items";
	private static final String COUPONS = "coupons";
	public static final BigDecimal TAX_RATE = new BigDecimal(".0825");
	private final Map<Long, Coupon> couponMap = new HashMap<>();

	@Value("classpath:coupons.json")
	private Resource couponResource;
	private ObjectMapper objectMapper;

	@Autowired
	private void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
	
	public Map<Long, Coupon> getCouponMap() {
		return couponMap;
	}

	/**
	 * Populates couponMap at the start of the application
	 */
	@Override
	public void afterPropertiesSet()  {
		try {
			File file = couponResource.getFile();
			Map<String, List<Coupon>> data = objectMapper.readValue(file, new TypeReference<>(){});
			List<Coupon> coupons = data.getOrDefault(COUPONS, Collections.emptyList());
			couponMap.putAll(coupons.stream().collect(
					Collectors.toMap(Coupon::getAppliedSku, v -> v, (k, d) -> d)));
		} catch (Exception e) {
			logger.error("There was a problem deserializing coupon.json");
		}		
		logger.info("Coupon Map: {}", couponMap);
	}

	/**
	 * Calculate different totals of a given shopping cart's items
	 * @param items JSON array of {@link Item Items}
	 * @return Map of Subtotal before discounts, Discount total, Subtotal after discounts, 
	 * 			Taxable subtotal after discounts, Tax total, and Grand total
	 */
	@PostMapping(value = "/calculate-totals", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, BigDecimal> calculateTotals(@RequestBody Map<String, List<Item>> items) {

		BigDecimal subTotal = new BigDecimal(0);
		BigDecimal taxableSubTotal = new BigDecimal(0);
		BigDecimal discountTotal = new BigDecimal(0);

		for (Item item : items.getOrDefault(ITEMS, Collections.emptyList())) {
			BigDecimal price = item.getPrice();
			Long itemSku = item.getSku();
			subTotal = subTotal.add(price);

			if (couponMap.containsKey(itemSku)) {
				discountTotal = discountTotal.add(couponMap.get(itemSku).getDiscountPrice());
			}
			
			if (item.getIsTaxable()) {
				taxableSubTotal = couponMap.containsKey(itemSku) ? 
						taxableSubTotal.add(price).subtract(couponMap.get(itemSku).getDiscountPrice()).max(BigDecimal.ZERO) : 
						taxableSubTotal.add(price);
			}
		}

		BigDecimal subTotalBeforeDiscount = subTotal;
		BigDecimal subTotalAfterDiscount = subTotal.subtract(discountTotal).max(BigDecimal.ZERO);
		BigDecimal taxTotal = taxableSubTotal.multiply(TAX_RATE);
		BigDecimal grandTotal = subTotalAfterDiscount.add(taxTotal);

		logger.info("Subtotal before discounts: {}", subTotalBeforeDiscount);
		logger.info("Discount Total: {}", discountTotal);
		logger.info("Subtotal after discounts: {}", subTotalAfterDiscount);
		logger.info("Taxable Sub Total: {}", taxableSubTotal);
		logger.info("Tax Total: {}", taxTotal);
		logger.info("Grand Total: {}", grandTotal);

		return Map.of("subTotalBeforeDiscount", subTotalBeforeDiscount, 
				"discountTotal", discountTotal, 
				"subTotalAfterDiscount", subTotalAfterDiscount, 
				"taxableSubTotal", taxableSubTotal, 
				"taxTotal", taxTotal, 
				"grandTotal", grandTotal);
	}
}
