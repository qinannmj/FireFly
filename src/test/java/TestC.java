import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class TestC {

	public static void main(String[] args) {
		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("1", "1");
		map.put("2", "2");
		map.put("3", "3");
		map.put("4", "4");
		Iterator<Entry<String, String>> iter = map.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, String> e = iter.next();
			map.remove("4");
			System.out.println(e.getKey());
		}
		
	}

}
