package com.fadx.predict_market_service.modules.common.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fadx.predict_market_service.core.datasource.trading.TradingDataSource;
import com.fadx.predict_market_service.modules.common.dto.OhlcvProjection;
import com.fadx.predict_market_service.modules.common.repositories.SymbolPriceHisRepository;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TradingServiceTest {
  private final TradingDataSource tradingDataSource = mock(TradingDataSource.class);
  private final SymbolPriceHisRepository repository = mock(SymbolPriceHisRepository.class);
  private final TradingService service = new TradingService(tradingDataSource, repository);

  @Test
  void returnsDatabaseHistoryWhenDailyCoverageIsSufficient() {
    LocalDate from = LocalDate.parse("2026-10-01");
    LocalDate to = LocalDate.parse("2026-10-30");
    List<OhlcvProjection> rows =
        java.util.stream.IntStream.range(0, 15)
            .mapToObj(index -> projection(from.plusDays(index)))
            .toList();
    when(repository.findOhlcvBySymbolAndDateRange("SSI", from, to.plusDays(1))).thenReturn(rows);

    OHLCVResponse response = service.getHistorical("SSI", from.toString(), to.toString(), "1D");

    assertThat(response.getS()).isEqualTo("SSI");
    assertThat(response.getT()).hasSize(15);
    verify(tradingDataSource, never()).historical("SSI", from.toString(), to.toString(), "1D");
  }

  @Test
  void fallsBackToMasWhenDailyCoverageIsIncomplete() {
    LocalDate from = LocalDate.parse("2026-10-01");
    LocalDate to = LocalDate.parse("2026-10-30");
    OHLCVResponse masResponse = OHLCVResponse.builder().s("SSI").build();
    OhlcvProjection incompleteRow = projection(from);
    when(repository.findOhlcvBySymbolAndDateRange("SSI", from, to.plusDays(1)))
        .thenReturn(List.of(incompleteRow));
    when(tradingDataSource.historical("SSI", from.toString(), to.toString(), "1D"))
        .thenReturn(masResponse);

    assertThat(service.getHistorical("SSI", from.toString(), to.toString(), "1D"))
        .isSameAs(masResponse);
  }

  @Test
  void sendsIntradayRequestDirectlyToMas() {
    OHLCVResponse masResponse = OHLCVResponse.builder().s("SSI").build();
    when(tradingDataSource.historical("SSI", "2026-10-01", "2026-10-30", "1"))
        .thenReturn(masResponse);

    assertThat(service.getHistorical("SSI", "2026-10-01", "2026-10-30", "1")).isSameAs(masResponse);
    verify(repository, never())
        .findOhlcvBySymbolAndDateRange(
            "SSI", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
  }

  @Test
  void rejectsReversedDateRange() {
    assertThatThrownBy(() -> service.getHistorical("SSI", "2026-10-30", "2026-10-01", "1D"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("toDate must not be before fromDate");
  }

  @Test
  void countsOnlyWeekdaysAndIncludesBothBounds() {
    assertThat(
            TradingService.countWeekdaysInclusive(
                LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-30")))
        .isEqualTo(10);
  }

  private static OhlcvProjection projection(LocalDate tradingDate) {
    OhlcvProjection row = mock(OhlcvProjection.class);
    when(row.getTradingDate()).thenReturn(tradingDate);
    when(row.getOpenPrice()).thenReturn(BigDecimal.ONE);
    when(row.getHighPrice()).thenReturn(BigDecimal.TWO);
    when(row.getLowPrice()).thenReturn(BigDecimal.ONE);
    when(row.getClosePrice()).thenReturn(BigDecimal.TWO);
    when(row.getVolume()).thenReturn(100L);
    return row;
  }
}
