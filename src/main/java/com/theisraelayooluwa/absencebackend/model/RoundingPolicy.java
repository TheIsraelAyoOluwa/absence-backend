package com.theisraelayooluwa.absencebackend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum RoundingPolicy {

    ROUND_UP {
        public BigDecimal apply(BigDecimal value) {
            return value.setScale(1, RoundingMode.CEILING);
        }
    },
    ROUND_DOWN {
        public BigDecimal apply(BigDecimal value) {
            return value.setScale(1, RoundingMode.FLOOR);
        }
    },
    ROUND_NEAREST_HALF_HOUR {
        public BigDecimal apply(BigDecimal value) {
            return value.multiply(BigDecimal.valueOf(2))
                    .setScale(0, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(2));
        }
    },
    NO_ROUNDING {
        public BigDecimal apply(BigDecimal value) {
            return value.setScale(4, RoundingMode.HALF_UP);
        }
    };

    public abstract BigDecimal apply(BigDecimal value);

}
