import com.sun.media.util.Registry;
import javax.media.PackageManager;

public class CreateBlankRegistry {
    public static void main(String args[]) {
	Registry r = new Registry();
	PackageManager.commitProtocolPrefixList();
	PackageManager.commitContentPrefixList();
	try {
	    r.commit();
	} catch (Throwable t) {
	    System.err.println(t);
	}
    }
}
