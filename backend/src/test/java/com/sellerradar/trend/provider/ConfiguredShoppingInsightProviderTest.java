package com.sellerradar.trend.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sellerradar.common.error.BusinessException;
import com.sellerradar.common.error.ErrorCode;
import com.sellerradar.common.external.provider.ExternalProviderDescriptor;
import com.sellerradar.common.external.provider.ExternalProviderMode;
import com.sellerradar.common.external.provider.NaverProviderCapabilities;
import com.sellerradar.trend.client.NaverApiHubDataLabClient;
import com.sellerradar.trend.client.NaverDataLabClient;
import com.sellerradar.trend.client.NaverDataLabKeywordTrendRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConfiguredShoppingInsightProviderTest {
	@Test
	void disabledModeNeverCallsEitherAdapter() {
		NaverProviderCapabilities capabilities = mock(NaverProviderCapabilities.class);
		NaverDataLabClient legacy = mock(NaverDataLabClient.class);
		NaverApiHubDataLabClient hub = mock(NaverApiHubDataLabClient.class);
		NaverDataLabKeywordTrendRequest request = mock(NaverDataLabKeywordTrendRequest.class);
		when(capabilities.descriptor()).thenReturn(new ExternalProviderDescriptor(
				ExternalProviderMode.DISABLED,
				Set.of()
		));
		ConfiguredShoppingInsightProvider provider = new ConfiguredShoppingInsightProvider(
				capabilities,
				legacy,
				hub
		);

		assertThatThrownBy(() -> provider.searchKeywordTrend(request))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.errorCode()).isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE));
		verify(legacy, never()).searchKeywordTrend(request);
		verify(hub, never()).searchKeywordTrend(request);
	}
}
