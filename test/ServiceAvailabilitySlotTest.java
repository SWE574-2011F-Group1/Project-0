import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

public class ServiceAvailabilitySlotTest extends UnitTest {
    @Test
    public void canCreateSlot() {
	Service service = new Service();
	ServiceAvailabilitySlot slot = new ServiceAvailabilitySlot(service, DayOfWeek.SATURDAY, 14, 30, 22, 50);

	assertEquals(DayOfWeek.SATURDAY, slot.dayOfWeek);
	assertEquals(870, slot.startTimeMinutesAfterMidnight);
	assertEquals(1370, slot.endTimeMinutesAfterMidnight);
    }
}
