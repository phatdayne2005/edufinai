package vn.uth.learningservice.dto.response;

import lombok.*;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageRes<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;
}
