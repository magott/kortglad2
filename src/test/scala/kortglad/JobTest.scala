package kortglad

import kortglad.Jobs.RefereeRefresherJob.OSLO

import java.time.{Instant, LocalDate, OffsetDateTime, ZoneId}

class JobTest extends munit.FunSuite {

  test("Creates correct stale date for referee referesh") {
    val expected = OffsetDateTime
      .now(ZoneId.of("Europe/Oslo"))
      .withMonth(1)
      .withDayOfMonth(1)
      .withHour(0)
      .withMinute(0)
      .withSecond(0)
      .withNano(0)
    assertEquals(Jobs.RefereeRefresherJob.staleDate, expected)
  }
}
