capture program drop wd
program wd
	version 13
	syntax anything
	
	gettoken project : anything
	
	global project_path : env `project'
	di "set wd to: $project_path"
	cd "$project_path"
end
