package com.fadx.predict_market_service.modules.common.utils;

import com.fadx.predict_market_service.modules.common.dto.OhlcvProjection;
import com.fadx.predict_market_service.modules.market.api.dtos.trading.OHLCVResponse;
import java.sql.Array;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

public class OhlcvHelper {

  public static OHLCVResponse toResponse(List<OhlcvProjection> rows, String symbol) {
    int n = rows.size();
    long[] t = new long[n];
    double[] o = new double[n];
    double[] h = new double[n];
    double[] l = new double[n];
    double[] c = new double[n];
    double[] v = new double[n];

    for (int i = 0; i < n; i++) {
      OhlcvProjection row = rows.get(i);
      t[i] = row.getTradingDate().atStartOfDay(ZoneOffset.UTC).toEpochSecond();
      o[i] = row.getOpenPrice().doubleValue();
      h[i] = row.getHighPrice().doubleValue();
      l[i] = row.getLowPrice().doubleValue();
      c[i] = row.getClosePrice().doubleValue();
      v[i] = row.getVolume();
    }

    return OHLCVResponse.builder().s(symbol).t(t).o(o).h(h).l(l).c(c).v(v).build();
  }

  public static OHLCVResponse toResponseFromColumnar(Map<String, Object> row, String symbol) {
    try {
      long[] t = toLongArray(row.get("t"));
      double[] o = toDoubleArray(row.get("o"));
      double[] h = toDoubleArray(row.get("h"));
      double[] l = toDoubleArray(row.get("l"));
      double[] c = toDoubleArray(row.get("c"));
      double[] v = toDoubleArray(row.get("v"));

      // Trường hợp không có dữ liệu, array_agg trả về NULL, không phải mảng rỗng
      if (t == null) {
        return OHLCVResponse.builder()
            .s(symbol)
            .t(new long[0])
            .o(new double[0])
            .h(new double[0])
            .l(new double[0])
            .c(new double[0])
            .v(new double[0])
            .build();
      }

      return OHLCVResponse.builder().s(symbol).t(t).o(o).h(h).l(l).c(c).v(v).build();

    } catch (SQLException e) {
      throw new RuntimeException("Failed to convert columnar OHLCV result", e);
    }
  }

  private static long[] toLongArray(Object rawArray) throws SQLException {
    if (rawArray == null) return null;
    Object[] boxed = (Object[]) ((Array) rawArray).getArray();
    long[] result = new long[boxed.length];
    for (int i = 0; i < boxed.length; i++) {
      result[i] = ((Number) boxed[i]).longValue();
    }
    return result;
  }

  private static double[] toDoubleArray(Object rawArray) throws SQLException {
    if (rawArray == null) return null;
    Object[] boxed = (Object[]) ((Array) rawArray).getArray();
    double[] result = new double[boxed.length];
    for (int i = 0; i < boxed.length; i++) {
      result[i] = ((Number) boxed[i]).doubleValue();
    }
    return result;
  }
}
