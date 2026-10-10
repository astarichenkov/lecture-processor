package com.lectureprocessor.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NoEvidenceDetectorTest {

    @Test
    void detectsCommonRussianRefusals() {
        assertThat(NoEvidenceDetector.isRefusal("В предоставленной лекции нет информации об этом.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal("Информации о погоде в Москве в предоставленных источниках нет.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal("В источниках не найдено ответа на вопрос.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal("Информации недостаточно для ответа.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal(
                "Информации о текущей погоде в Москве в источниках недостаточно.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal("В источниках не указан автор лекции.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal("Ответ не содержится в источниках.")).isTrue();
        assertThat(NoEvidenceDetector.isRefusal("")).isTrue();
    }

    @Test
    void doesNotTreatGroundedAnswersAsRefusal() {
        assertThat(NoEvidenceDetector.isRefusal("Вектор — это направленный отрезок, имеющий длину и направление."))
                .isFalse();
        assertThat(NoEvidenceDetector.isRefusal("Смешанное произведение трёх векторов равно объёму параллелепипеда."))
                .isFalse();
    }
}
