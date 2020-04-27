package me.nov.dalvikgate.transform;

import java.util.Set;

import org.jf.dexlib2.dexbacked.value.DexBackedArrayEncodedValue;
import org.jf.dexlib2.iface.Annotation;

import me.nov.dalvikgate.values.ValueBridge;

public interface ITransformer<T> {
	public void build();

	public T get();

	public static final String SIGNATURE_ANNOTATION = "Ldalvik/annotation/Signature;";

	default String getSignature(Set<? extends Annotation> set) {
		//TODO
		Annotation signature = set.stream().filter(a -> a.getType().equals(SIGNATURE_ANNOTATION)).findFirst().orElse(null);
		if (signature != null) {
			DexBackedArrayEncodedValue sig = (DexBackedArrayEncodedValue) signature.getElements().iterator().next().getValue();
			return ValueBridge.arrayToString(sig);
		}
		return null;
	}
}
