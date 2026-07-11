package com.sellerradar.auth.session;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;

final class RefreshSessionRejectedException extends BusinessException {
	RefreshSessionRejectedException() {
		super(ErrorCode.INVALID_REFRESH_TOKEN);
	}
}
