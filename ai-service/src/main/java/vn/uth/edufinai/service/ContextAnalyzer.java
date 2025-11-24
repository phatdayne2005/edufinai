package vn.uth.edufinai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service để phân tích context để quyết định service nào cần gọi
 * Implement Selective Data Sending strategy để tối ưu token usage
 * 
 * Chỉ xử lý theo context, không phân tích keywords trong question
 */
@Slf4j
@Service
public class ContextAnalyzer {

    /**
     * Phân tích context để quyết định service nào cần gọi
     * 
     * @param context Preset context (SPENDING_WIDGET, SAVING_WIDGET, GOAL_WIDGET, etc.)
     * @return RequiredServices chứa thông tin service nào cần gọi
     */
    public RequiredServices analyzeRequiredServices(String context) {
        RequiredServices services = new RequiredServices();
        
        // Nếu có context, chỉ gọi service tương ứng
        if (context != null && !context.trim().isEmpty()) {
            String upperContext = context.trim().toUpperCase();
            switch (upperContext) {
                case "SPENDING_WIDGET":
                    services.needFinance = true;
                    log.info("Context SPENDING_WIDGET: chỉ gọi Finance service");
                    return services;
                    
                case "SAVING_WIDGET":
                case "GOAL_WIDGET":
                    // Cả hai widget đều cần Finance + Gamification
                    services.needFinance = true;
                    services.needGamification = true;
                    log.info("Context {}: gọi Finance + Gamification services", upperContext);
                    return services;
                    
                default:
                    // Context không xác định, gọi tất cả (fallback)
                    log.info("Context không xác định: {}, gọi tất cả services", upperContext);
                    break;
            }
        }
        
        // Nếu không có context, gọi tất cả services (fallback để đảm bảo đủ dữ liệu)
        services.setAllRequired();
        log.info("Không có context, gọi tất cả services (fallback)");
        
        return services;
    }
    
    /**
     * Class chứa thông tin service nào cần gọi
     */
    public static class RequiredServices {
        public boolean needFinance = false;
        public boolean needLearning = false;
        public boolean needGamification = false;
        
        /**
         * Đặt tất cả services là required
         */
        public void setAllRequired() {
            needFinance = true;
            needLearning = true;
            needGamification = true;
        }
        
        /**
         * Kiểm tra xem có cần gọi service nào không
         */
        public boolean hasAnyRequired() {
            return needFinance || needLearning || needGamification;
        }
        
        /**
         * Đếm số lượng service cần gọi
         */
        public int count() {
            int count = 0;
            if (needFinance) count++;
            if (needLearning) count++;
            if (needGamification) count++;
            return count;
        }
    }
}

