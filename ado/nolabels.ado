capture program drop nolabels
program nolabels
	version 13
	syntax [varlist]
	
	local varcount : word count `varlist'
	di "`varcount'"
	
	if `varcount' == 0 {
		foreach var of varlist _all {
			label variable `var' ""
		}
	}
	else {
		foreach var of varlist `varlist' {
			label variable `var' ""
		}
	}
end
