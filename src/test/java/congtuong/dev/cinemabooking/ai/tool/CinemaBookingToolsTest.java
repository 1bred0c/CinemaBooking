package congtuong.dev.cinemabooking.ai.tool;

import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeSearchToolResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class CinemaBookingToolsTest {

    @Test
    void searchRejectsInvalidDateWithoutCallingDatabaseService() {
        CinemaBookingTools tools = new CinemaBookingTools(
                mock(CinemaToolQueryService.class)
        );

        ShowtimeSearchToolResponse response = tools.searchShowtimes(
                "Interstellar",
                null,
                "02/08/2026",
                "EVENING"
        );

        assertFalse(response.success());
        org.junit.jupiter.api.Assertions.assertEquals(
                List.of(),
                response.showtimes()
        );
    }
}
