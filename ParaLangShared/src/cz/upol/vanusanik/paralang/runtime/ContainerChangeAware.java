package cz.upol.vanusanik.paralang.runtime;

public interface ContainerChangeAware {
	
	void ___couple(ContainerChangeAware container);
	void ___decouple(ContainerChangeAware container);
	void __update(ContainerChangeAware owner);
	
}
