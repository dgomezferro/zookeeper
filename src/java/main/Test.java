import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
				Charset charset = Charset.forName("US-ASCII");
				try{
					Path path = FileSystems.getDefault().getPath("zookeper_trace.log");
					BufferedReader reader = Files.newBufferedReader(path, charset);
					String line = null;
					int lastVal = 0;
					int phase = 0;
					int lines = 0;
					while ((line = reader.readLine()) != null) {
						lines ++;
						if(line.matches(".*[Oo]utstanding.*")){
							lastVal = lines;
							if (phase == 1 && lines != (lastVal + 3)){
								System.out.println("Check line " + lines);
							}
							else if (phase == 2 && lines != (lastVal + 1)){
								System.out.println("Check line " + lines);
							}
							phase ++;
							if (phase == 3){
								phase = 0;
							}
						}
					}
					if (phase != 0){
						System.out.println("Check line " + lastVal);
					}
				} catch (IOException x) {
					System.err.format("IOException: %s%n", x);
				}
			}

}
