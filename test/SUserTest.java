
import org.junit.*;
import java.util.*;
import play.test.*;
import models.*;

public class SUserTest extends UnitTest {
    @Test
    public void upvoteForTask_works() {
    	SUser suser = new SUser("Guybrush Threepwood", "guybrush@foo.com");
    	Task task = new Task("Cleaning", 5);
    	
    	suser.upvoteForTask(task);

        assertEquals(1, task.voteDiff);
    }
}
