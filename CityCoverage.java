
import static java.lang.Math.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.util.BitSet;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.regex.Pattern;
import java.awt.Desktop;

//The code here is mostly data marshaling to read a list of cities, reduce it into dominating set problem, receive the solution, and turn it back into something meaningful to humans
class CityCoverage{
	static class City{
		public static final double earth_radius = 6378.137;
		public double lat;
		public double lon;
		public String name;
		public City(String name, double latitude, double longitude){
			this.name = name;
			this.lat = latitude;
			this.lon = longitude;
		}
		public double great_circle_distance(City other)
		{
			return 2.0*earth_radius*asin(sqrt(pow(sin(abs(toRadians(this.lat-other.lat))/2),2) + 
				(cos(toRadians(this.lat))*cos(toRadians(other.lat))*(pow(sin(abs(toRadians(this.lon-other.lon))/2),2))) ));
		}
		public String toString(){
			return this.name;
		}
	}

	public List<City> cities;
	public double radius;
	public CityCoverage(String filename, double radius){
		this.cities = read_csv(filename);	
		this.radius = radius;
	}

	List<City> read_csv(String filename){
		List<City> positions = new ArrayList<>();
		try(BufferedReader br = new BufferedReader(new FileReader(filename))){
			String line;
			for(int i=0; (line=br.readLine())!=null; i++){
				String[] tokens = line.split(";");
				double lat = Double.parseDouble(tokens[6]);
				double lon = Double.parseDouble(tokens[7]);
				City c = new City(tokens[0], lat, lon);
				positions.add(c);
			}
		}
		catch(Exception e){
			System.out.println(e);
			System.exit(-1);
		}
		return positions;
	}

	List<BitSet> generate_adjacency(double radius){
		List<BitSet> adjacency = new ArrayList<>();
		for(int i=0; i<cities.size(); i++){
			adjacency.add(new BitSet());
			adjacency.get(i).set(i);
			for(int j=0; j<i; j++){
				if(cities.get(i).great_circle_distance(cities.get(j))<=radius){
					adjacency.get(i).set(j);
					adjacency.get(j).set(i);
				}
			}
		}
		return adjacency;
	}

	BitSet solve_cities(){
		List<BitSet> adjacency = generate_adjacency(radius);
		DominatingSet solver = new DominatingSet(adjacency);
		return solver.solve();
	}

	List<City> interpret_solution(BitSet solution){
		List<City> output = new ArrayList<>();
		for(int i=solution.length(); (i=solution.previousSetBit(i-1))>=0;){
			output.add(cities.get(i));
		}
		return output;
	}

	void draw_solution(BitSet solution){
		List<String> cities_json = new ArrayList<>();
		for(int i=0; i<this.cities.size(); i++){
			cities_json.add(
				String.format("{name:\"%s\",lat:%f,lon:%f,selected:%b}",
							cities.get(i).name, cities.get(i).lat, cities.get(i).lon, solution.get(i))
			);
		}
		String output_file = "maps/CoverageMap.html";
		try{
			String contents = Files.readString(Path.of("CoverageMapTemplate.html"));
			String[] segments = contents.split(Pattern.quote("/*RADIUS*/"));
			segments[1] = String.valueOf(radius);
			contents = String.join("/*RADIUS*/",segments);
			segments = contents.split(Pattern.quote("/*CITIES*/"));
			segments[1] = cities_json.toString();
			contents = String.join("/*CITIES*/",segments);
			Path outfile = Path.of(output_file);
			if(outfile.getParent() != null){
				Files.createDirectories(outfile.getParent());
			}
			Files.writeString(outfile, contents);
		}
		catch(Exception e){
			System.out.println("Template file error:");
			System.out.println(e);
			return;
		}
		try{
			Desktop dt = Desktop.getDesktop();
			dt.open(new File(output_file));
		}
		catch(Exception e){
			System.out.printf("Error opening %s:\n",output_file);
			System.out.println(e);
		}
	}

	public static void main(String[] args){
		String filename = "data/worldcities.csv";
		double radius = 1000;
		if(args.length>0){
			filename = args[0];
			if(args.length>1){
				radius = Double.parseDouble(args[1]);
			}
		}
		CityCoverage csc = new CityCoverage(filename, radius);
		long start_time = System.nanoTime();
		BitSet selected = csc.solve_cities();
		long end_time = System.nanoTime();
		List<City> solution = csc.interpret_solution(selected);
		System.out.println(solution.size());
		System.out.println(solution);
		System.out.print((end_time-start_time)/pow(10,9));
		System.out.println(" seconds");
		csc.draw_solution(selected);
	}

}