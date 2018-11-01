capture program drop panelevent
capture program panelevent
	version 15
	syntax varlist(min=2 max=2), k(integer) l(integer) panel(varname) time(varname)
	
	javacall de.pbc.stata.PanelEventStudy start `varlist', classpath("C:\Users\corne\Git\stata-utils\bin") args(`k' `l' `panel' `time')
end
