package vn.uth.financeservice.dto;

import java.math.BigDecimal;

public class CategorySummaryDto {
    private String cat; // categoryName
    private BigDecimal amt; // amount
    private Long cnt; // count
    private Double pct; // percent

    public CategorySummaryDto() {}

    public CategorySummaryDto(String cat, BigDecimal amt, Long cnt, Double pct) {
        this.cat = cat;
        this.amt = amt;
        this.cnt = cnt;
        this.pct = pct;
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }

    public BigDecimal getAmt() {
        return amt;
    }

    public void setAmt(BigDecimal amt) {
        this.amt = amt;
    }

    public Long getCnt() {
        return cnt;
    }

    public void setCnt(Long cnt) {
        this.cnt = cnt;
    }

    public Double getPct() {
        return pct;
    }

    public void setPct(Double pct) {
        this.pct = pct;
    }
}

