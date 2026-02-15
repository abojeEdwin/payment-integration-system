package com.paybridge.payment.domain.model;

import java.math.BigDecimal;


public class Transaction {
		private long id;
		private String merchantId;
		private String psp;
		private String reference;
		private BigDecimal amount;
		private String currency;
		private String status;
		private String customerEmail;

		public Transaction(String merchantId, String psp, String reference, BigDecimal amount, String currency, String status, String customerEmail) {
				this.merchantId = merchantId;
				this.psp = psp;
				this.reference = reference;
				this.amount = amount;
				this.currency = currency;
				this.status = status;
				this.customerEmail = customerEmail;
		}

		public long id() {
				return id;
		}

}