package cz.upol.vanusanik.paralang.cli;

import com.beust.jcommander.JCommander;

public class ParaLangCLI {

	public static void main(String[] args) {
		ParaLangCLIOptions no = new ParaLangCLIOptions();
		new JCommander(no, args);
		
		run(no);
	}

	private static void run(ParaLangCLIOptions no) {
		
	}

}
