package edu.upol.vanusanik.node;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class MinimumOneValidator implements IParameterValidator {

	@Override
	public void validate(String arg0, String arg1) throws ParameterException {
		try {
			int value = Integer.parseInt(arg1);
			if (value <= 1)
				throw new ParameterException("Minimal amount of helper threads must be at least 1");
 		} catch (NumberFormatException e){
			throw new ParameterException("Not a number");
		}
	}

}
