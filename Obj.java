import java.util.List;
import java.util.stream.Stream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.Float;

public class Obj {
    private List<Float[]> vertices = new ArrayList<Float[]>();
    private List<Float[]> uvCoords = new ArrayList<Float[]>();
    private List<int[]> faceVertexIndeces = new ArrayList<int[]>();
    private List<int[]> faceUVIndeces = new ArrayList<int[]>();
    private List<String> faceTextures = new ArrayList<String>();
    private HashMap<String, String> materialTextures = new HashMap<String, String>();
    
    public Obj() {
    }
    //if vertex world positions and face indexes have aleady been defined
    public Obj(List<Float[]> objVertices, List<int[]> faceIndeces) {
        vertices = objVertices;
        faceVertexIndeces = faceIndeces;
    }
    
    public Obj(String objSrc) throws IOException {
        String mtlSrc = "";

        BufferedReader br = new BufferedReader(new FileReader(objSrc));
        String currentTexturePath = "textures/defaultmissing.png";
        String line = br.readLine();
        while (line != null) {
            switch (line.substring(0, line.indexOf(" "))) {
                case "v":
                    //vert will look something like "-1.000000 1.000000 5.000000" 
                    String[] vertStringArray = line.substring(line.indexOf(" ") + 1).split(" ");
                    //so we can split them into a String[] by spaces and map each one to a float,
                    //then add it to vertices[]
                    Float[] vertXYZ = Stream.of(vertStringArray).map(Float::valueOf).toArray(Float[]::new);
                    vertices.add(vertXYZ);
                    break;
                case "vn": break; //no plan to use these yet
                case "vt": 
                    //vert will look something like "-1.000000 1.000000 5.000000" 
                    String[] uvStringArray = line.substring(line.indexOf(" ") + 1).split(" ");
                    //so we can split them into a String[] by spaces and map each one to a float,
                    //then add it to vertices[]
                    Float[] uvXY = Stream.of(uvStringArray).map(Float::valueOf).toArray(Float[]::new);
                    uvCoords.add(uvXY);
                    break;
                case "mtllib": //path to the mtl used by the obj
                    String folderPath = objSrc.substring(0, objSrc.lastIndexOf("/") + 1); //path of the obj file since the path of the mtl is relative to it
                    mtlSrc = folderPath + line.substring(7);
                    loadMTL(mtlSrc);
                    break;
                case "usemtl":
                    String materialName = line.substring(line.indexOf(" ") + 1);
                    
                    System.out.println(materialName);
                    currentTexturePath = materialTextures.get(materialName);
                    break;
                case "f": 
                    //faceIndexesArray will look something like "1/1/1, 5/5/1, 7/9/1, 3/3/1", we only need the first two numbers in each
                    //of those groups because the other refers to the "vn ", vertex normal, which we dont need yet
                    String[] faceIndexesArray = line.substring(line.indexOf(" ") + 1).split(" ");
                    //same as case "v ", mapping to a float then adding it to faceVertexIndeces[]
                    int[] faceIndexes = Stream.of(faceIndexesArray).mapToInt(f -> fNumberAtIndex(0, f)).toArray();
                    int[] uvIndexes = Stream.of(faceIndexesArray).mapToInt(f -> fNumberAtIndex(1, f)).toArray();
                    faceVertexIndeces.add(faceIndexes);
                    faceUVIndeces.add(uvIndexes);
                    faceTextures.add(currentTexturePath);
                    break;
            }
            line = br.readLine();
        }
        br.close();
    }
    //if an mtl is referenced, add the paths to texture files to a hash using the materials theyre associated with,
    //will be one material per face
    public void loadMTL(String mtlSrc) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(mtlSrc));
        String line = br.readLine();
        String currentMaterialName = "";
        while (line != null) {
            int spaceIndex = line.indexOf(" ");
            if (spaceIndex != -1) {
                switch (line.substring(0, spaceIndex)) {
                    case "newmtl":
                        currentMaterialName = line.substring(spaceIndex + 1);
                        break;

                    case "map_Kd":
                        String texturePath = line.substring(spaceIndex + 1);
                        materialTextures.put(currentMaterialName, texturePath);
                        Game.addTextureToRepo(texturePath);
                        break;
                }
            }
            
            line = br.readLine();
        }
        br.close();
    }
    //get the number at index 'index' in an fline (ex: "1/1/1" or "5/5/1")
    public int fNumberAtIndex(int index, String fLine) {
        int lastSlash = 0;

        for(int i = 0; i < fLine.length(); i++) {
            if (fLine.charAt(i) == '/') {
                index--;

                if (index == -1) {
                    return Integer.parseInt(fLine.substring(lastSlash, i));
                } else {
                    lastSlash = i + 1;
                }
            }
        }
        return -1;
    }

    public List<Float[]> getVertices() {
        return vertices;
    }
    public List<Float[]> getUVCoords() {
        return uvCoords;
    }
    public List<int[]> getFaceIndexes() {
        return faceVertexIndeces;
    }
    public List<int[]> getUVIndexes() {
        return faceUVIndeces;
    }
    public List<String> getTexturePaths() {
        return faceTextures;
    }

    @Override
    public String toString() {
        List<String> vertTemp = new ArrayList<String>();
        vertices.forEach(vert -> vertTemp.add(Arrays.toString(vert)));

        List<String> faceTemp = new ArrayList<String>();
        faceVertexIndeces.forEach(face -> faceTemp.add(Arrays.toString(face)));

        return vertTemp.toString() + ",\n " + faceTemp;
    }
}