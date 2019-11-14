package de.pbc.stata;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.stata.sfi.Data;
import com.stata.sfi.Macro;
import com.stata.sfi.SFIToolkit;
import com.stata.sfi.ValueLabel;

/*
 * - Generates semi-dynamic panel event study variables for a given
 *   event variable.
 * - K >= 1 is the number of repetitions (repetitions > K are binned)
 * - L >= 0 is the number of lags (lags > L are binned)
 * - The panel needs to be sorted.
 */
public class PanelEventStudy {
	
	// VARIABLES ------------------------------------------------- //
	
	private SortedSet<Integer> states;
	
	private int K, L, eventVarIndex, stateVarIndex, panelVarIndex, timeVarIndex;
	
	private String eventVarName, panelVarName, timeVarName;
	
	private Map<Integer, Map<Integer, Map<Integer, String>>> varNames;
	
	// CONSTRUCTOR ----------------------------------------------- //
	
	public static int start(String[] args) {
		return new PanelEventStudy(args).execute();
	}
	
	public PanelEventStudy(String[] args) {
		K = Integer.valueOf(args[0]);
		if (K < 1) {
			throw new RuntimeException(String.format("K (%d) cannot be < 1", K));
		}
		
		L = Integer.valueOf(args[1]);
		if (L < 0) {
			throw new RuntimeException(String.format("L (%d) cannot be < 0", L));
		}
		
		eventVarIndex = Data.mapParsedVarIndex(1);
		eventVarName = Data.getVarName(eventVarIndex);
		
		stateVarIndex = Data.mapParsedVarIndex(2);
		getStates();
		
		panelVarName = args[2];
		panelVarIndex = Data.getVarIndex(panelVarName);
		
		timeVarName = args[3];
		timeVarIndex = Data.getVarIndex(timeVarName);
		
		SFIToolkit.executeCommand("global sorted :sortedby", false);
		String[] sortedVars = Macro.getGlobal("sorted").split(" ");
		if (sortedVars.length < 2 || !sortedVars[0].equals(panelVarName) || !sortedVars[1].equals(timeVarName)) {
			throw new RuntimeException(String.format("data not sorted for panel (%s) and time variables (%s)",
					panelVarName,
					timeVarName));
		}
	}
	
	// PUBLIC ---------------------------------------------------- //
	
	public int execute() {
		generateVars();
		fillVars();
		return 0;
	}
	
	// PRIVATE --------------------------------------------------- //
	
	private void getStates() {
		states = new TreeSet<>();
		long obs = Data.getObsTotal();
		for (long i = 1; i <= obs; i++) {
			double val = Data.getNum(stateVarIndex, i);
			if (!Data.isValueMissing(val)) {
				states.add((int) val);
			}
		}
		varNames = new HashMap<>(states.size());
	}
	
	private String getVarName(Integer state, int k, int l) {
		return varNames.computeIfAbsent(state, key -> new HashMap<>(K))
				.computeIfAbsent(k, key -> new HashMap<>(L + 1))
				.computeIfAbsent(l, key -> String.format("%s_%d_%d_%d", eventVarName, state, k, l));
	}
	
	private void generateVars() {
		// remove all existing variables that may be overwritten
		SFIToolkit.executeCommand(String.format("drop %s_*", eventVarName), false);
		
		Map<Integer, String> stateLabels = ValueLabel.getValueLabels(ValueLabel.getVarValueLabel(stateVarIndex));
		if (stateLabels.isEmpty())
			stateLabels = null;
		
		for (Integer state : states) {
			for (int k = 1; k <= K; k++) {
				for (int l = 0; l <= L; l++) {
					SFIToolkit.displayln(String.format("s=%d k=%d l=%d", state, k, l));
					
					String varName = getVarName(state, k, l);
					Data.addVarByte(varName);
					SFIToolkit.executeCommand(String.format("quietly: replace %s = 0", varName), false);
					
					if (stateLabels != null) {
						Data.setVarLabel(Data.getVarIndex(varName),
								String.format("%s %s %s=%d %s=%d",
										eventVarName,
										Optional.ofNullable(stateLabels.get(state)).orElse(state.toString()),
										k == K ? "K" : "k",
										k,
										l == L ? "L" : "l",
										l));
					} else {
						Data.setVarLabel(Data.getVarIndex(varName),
								String.format("%s s=%d %s=%d %s=%d",
										eventVarName,
										state,
										k == K ? "K" : "k",
										k,
										l == L ? "L" : "l",
										l));
					}
				}
			}
		}
	}
	
	private void fillVars() {
		long panelId = -1;
		Map<Integer, Integer> eventCounter = null;
		Map<Integer, Map<Integer, Integer>> lastEvent = null;
		
		long obs = Data.getObsTotal();
		for (long i = 1; i <= obs; i++) {
			if (i % 1000000 == 0) {
				SFIToolkit.displayln(i + "/" + obs + " obsverations");
			}
			
			if (i == 1 || panelId != StataUtils.getLong(panelVarIndex, i)) {
				panelId = StataUtils.getLong(panelVarIndex, i);
				eventCounter = new HashMap<>();
				lastEvent = new HashMap<>();
			}
			
			int timeId = StataUtils.getInt(timeVarIndex, i);
			Integer rowState = StataUtils.getIntObj(stateVarIndex, i);
			Integer event = StataUtils.getIntObj(eventVarIndex, i); // TODO: shouldn't the state be lagged?
			
			// continue if state or event are missing values
			if (rowState == null || event == null) {
				continue;
			}
			
			// the event variable needs to be 1 for an event to occur
			if (event.equals(1)) {
				eventCounter.merge(rowState, 1, (k1, k2) -> Math.min(k1 + k2, K));
				lastEvent.computeIfAbsent(rowState, HashMap::new).putIfAbsent(eventCounter.get(rowState), timeId);
			}
			
			for (Integer state : states) {
				if (eventCounter.containsKey(state)) {
					for (int k = 1; k <= eventCounter.get(state); k++) {
						for (int l = 0; l <= L; l++) {
							// binning happens automatically
							if (Math.min(timeId - lastEvent.get(state).get(k), L) == l) {
								Data.storeNum(Data.getVarIndex(getVarName(state, k, l)), i, (double) 1);
							}
						}
					}
				}
			}
		}
	}
	
}