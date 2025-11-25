package vn.uth.learningservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.learningservice.dto.response.AiListLessonRes;
import vn.uth.learningservice.dto.response.LessonAiRes;
import vn.uth.learningservice.model.Lesson;
import vn.uth.learningservice.service.LessonService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class AiIntegrationController {

        private final LessonService lessonService;

        /**
         * GET /api/learning/ai-response
         * 
         * Trả về danh sách bài học đã được phê duyệt và xuất bản cho AI service.
         * Endpoint này yêu cầu JWT authentication.
         * 
         * @return Danh sách bài học với format đơn giản (lessonId, title, description)
         */
        @GetMapping("/ai-response")
        public ResponseEntity<AiListLessonRes> getAiResponse() {
                // Lấy danh sách bài học đã được phê duyệt và xuất bản
                List<Lesson> publishedLessons = lessonService.listPublished();

                // Map từ Lesson sang LessonAiRes
                List<LessonAiRes> lessonAiResList = publishedLessons.stream()
                                .map(lesson -> LessonAiRes.builder()
                                                .lessonId(lesson.getId())
                                                .title(lesson.getTitle())
                                                .description(lesson.getDescription())
                                                .build())
                                .collect(Collectors.toList());

                // Tạo response
                AiListLessonRes response = AiListLessonRes.builder()
                                .lessons(lessonAiResList)
                                .build();

                return ResponseEntity.ok(response);
        }
}
