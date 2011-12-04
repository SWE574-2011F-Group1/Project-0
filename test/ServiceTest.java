import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

public class ServiceTest extends UnitTest {
    @Test
    public void canAddSlot() {
	Service service = new Service();
	service.addSlot(DayOfWeek.WEDNESDAY, 12, 35, 22, 45);

        assertEquals(1, service.slots.size());
        assertEquals(DayOfWeek.WEDNESDAY, service.slots.get(0).dayOfWeek);
        assertEquals(12, service.slots.get(0).startTimeHour);
    }
}
