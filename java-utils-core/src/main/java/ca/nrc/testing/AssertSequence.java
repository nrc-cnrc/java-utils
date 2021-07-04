package ca.nrc.testing;

import ca.nrc.datastructure.Cloner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;

import java.util.*;

public class AssertSequence<T> extends Asserter<T[]> {


	public AssertSequence(T[] _seq) {
		super(_seq);
	}

	public AssertSequence(T[] _seq, String mess) {
		super(_seq, mess);
	}

	public AssertSequence(List<T> _sequenceLst) {
		super(null);
		T[] seqArr = _sequenceLst.toArray(sequence().clone());
		this.gotObject = seqArr;
	}

	public void startsWith(T... expHeadSet) throws Exception {
		startsWith(null, expHeadSet);
	}

	public void startsWith(Boolean anyOrder, T... expHeadSet) throws Exception {
		if (anyOrder == null) {
			anyOrder = false;
		}

		if (anyOrder) {
			startsWith_AnyOrder(expHeadSet);
		} else {
			startsWith_OrderSensitive(expHeadSet);
		}
	}

	private void startsWith_AnyOrder(T[] expHead) throws Exception {
		Set<T> expHeadSet = new HashSet<T>();
		Collections.addAll(expHeadSet, expHead);
		Set<T> gotHeadSet = new HashSet<T>();
		if (sequence().length < expHeadSet.size()) {
			Assertions.fail(
				baseMessage+
				"\nSequence contained less items than the expected head.\n"+
				"\nExp Head Set :\n"+set2string(expHeadSet)+"\n"+
				"\nGot Sequ     :\n"+seq2string(sequence())
				);
		}
		int headLength = Math.min(sequence().length, expHeadSet.size());
		Collections.addAll(
			gotHeadSet, Arrays.copyOfRange(sequence(), 0, headLength));
		AssertObject.assertDeepEquals(
			baseMessage+"\nSequence started with wrong head",
			expHeadSet, gotHeadSet);
	}

	public void startsWith_OrderSensitive(T... expHead) throws Exception {
		if (sequence().length < expHead.length) {
			Assertions.fail(
				baseMessage+
				"\nSequence contained less items than the expected head.\n"+
				"\nExp Head :\n"+seq2string(expHead)+"\n"+
				"\nGot Sequ :\n"+seq2string(sequence())
			);
		}
		int headLength = Math.min(sequence().length, expHead.length);
		T[] gotHead = Arrays.copyOfRange(sequence(), 0, headLength);
		AssertObject.assertDeepEquals(
			baseMessage+"\nSequence started with wrong head",
			expHead, gotHead);
	}

	protected T[] sequence() {
		return (T[])gotObject;
	}

	protected String set2string(Set<T> set) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(set);
	}

	protected String seq2string(T[] seq) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(seq);
	}
}
