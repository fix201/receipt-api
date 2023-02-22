package com.heb.cart.receipt;

import com.heb.cart.receipt.entity.Coupon;
import com.heb.cart.receipt.entity.Item;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
class ReceiptApiControllerTest {

	@Autowired
	private ReceiptApiController receiptApiController;

	@Test
	public void couponMapLoads() {
		HashMap<Long, Coupon> expectedCouponMap = new HashMap<>();
		expectedCouponMap.put(30532705L, new Coupon("Spaghetti Discount",30532705L,new BigDecimal("1.83")));
		expectedCouponMap.put(61411728L, new Coupon("Tofurky Discount",61411728L,new BigDecimal("1.01")));
		expectedCouponMap.put(21411389L, new Coupon("Seafood Discount",21411389L,new BigDecimal("1.50")));
		expectedCouponMap.put(85294241L, new Coupon("Brownie Discount",85294241L,new BigDecimal("0.79")));

		Assertions.assertEquals(expectedCouponMap, receiptApiController.getCouponMap());
	}
	
	@SneakyThrows
	@Test
	public void calculateTotalsTest() {
		Map<String, List<Item>> requestBody = getItemsRequestBody();
		
		BigDecimal subTotal = new BigDecimal("31.33");
		BigDecimal taxableSubTotal = new BigDecimal("14.94");
		BigDecimal discountTotal = new BigDecimal("0.79");
		BigDecimal subTotalAfterDiscount = subTotal.subtract(discountTotal).max(BigDecimal.ZERO);
		BigDecimal taxTotal = taxableSubTotal.multiply(ReceiptApiController.TAX_RATE);
		BigDecimal grandTotal = subTotalAfterDiscount.add(taxTotal);

		Map<String, BigDecimal> responseObject = Map.of("subTotalBeforeDiscount", subTotal,
				"discountTotal", discountTotal,
				"subTotalAfterDiscount", subTotalAfterDiscount,
				"taxableSubTotal", taxableSubTotal,
				"taxTotal", taxTotal,
				"grandTotal", grandTotal);

		Assertions.assertEquals(responseObject, receiptApiController.calculateTotals(requestBody));
	}
	
	public Map<String, List<Item>> getItemsRequestBody() {
		Map<String, List<Item>> itemsRequestBody = new HashMap<>();
		itemsRequestBody.put("items",List.of(new Item("H-E-B Two Bite Brownies",85294241L,false,true, new BigDecimal("3.61")),
				new Item("Halo Top Vanilla Bean Ice Cream",95422042L,false,false,new BigDecimal("3.31")),
				new Item("H-E-B Select Ingredients Creamy Creations Vanilla Bean Ice Cream",64267055L,true,true,new BigDecimal("9.83")),
				new Item("Aveeno Daily Moisturizing Body Wash",12821859L,true,false,new BigDecimal("5.11")),
				new Item("Hershey's Chocolate Syrup",23991994L,false,false,new BigDecimal("9.47"))
				));
		return itemsRequestBody;
	}

}
