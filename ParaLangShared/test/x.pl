using SecondModule.Class4;
import java.util.Map;

module HelloWorldModule {
	class Helper {
	
		var x = "Hello, world!", y = 3.14;
		var a, b, c;
		
		foo(bar){
			var b = new Helper();
			var c = new Class4();
		}
		
		restricted baz(){
			var y = k;
		}
		
	}
};