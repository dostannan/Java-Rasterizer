List<Integer> faceRenderOrder = new ArrayList<Integer>();
// List<Float> faceDistances = new ArrayList<Float>();
// List<Integer> colors = new ArrayList<Integer>();
// int sizeFDR = 0; 

for (Obj object : objsToRender) {
    final Float[] bounds = object.height();
    final List<Float[]> points = object.getVertices();
    final List<int[]> faceIndecies = object.getFaceIndexes();
    List<Float[]> screenPoints = deepCopyList(points); //copy list
    //convert each point in list from world coords to screen pane coords

    screenPoints = screenPoints.stream().map(point -> convertToScreenPos(cameraOrigin, cameraRot, cameraFov, point, true, true, currentFaceColors)).collect(Collectors.toList());
    //graph on screen
    if (screenPoints.size() == 1) {
        Float[] vert1 = screenPoints.get(0); 
        if(vert1[0] == null) {
            continue;
        }
        g.setColor(currentLineColor);
        g.fillOval(Math.round(vert1[0]), Math.round(vert1[1]), 5, 5);
    }
    OUTERLOOP: for(int[] faceVerts : faceIndecies) {
        // for(int i = 0; i < faceVerts.length; i++) {
        //     //get point in screenPoints at the index faceVerts[i] (subtracted by one because most .obj files start their
        //     //indexing at 1 not 0), and also the point one index ahead of it, then connects them with a line
        //     Float[] vert1 = screenPoints.get(faceVerts[i] - 1);
        //     Float[] vert2 = screenPoints.get(faceVerts[i < (faceVerts.length - 1) ? i + 1 : 0] - 1);
        //     //culls point if its outside of the screen
        //     if (vert1[0] == null || vert2[0] == null) {
        //             continue;
        //     }

        //     // g.setColor(currentLineColors.get(i));
        
        //     g.drawLine(Math.round(vert1[0]), Math.round(vert1[1]), Math.round(vert2[0]), Math.round(vert2[1]));
        // }
        int length = faceVerts.length;
        Float colorAvg = 0f;

        Float[] origin = {0f, 0f, 0f};
        // Float centerY = 0f;
        // int index = faceIndecies.indexOf((faceVerts));

        // if(index >= currentFaceColors.size()) {
        //     index = 0;
        // }

        int[] faceXPoints = new int[length];
        int[] faceYPoints = new int[length];

        for(int i = 0; i < length; i++) {
            Float[] vert = screenPoints.get(faceVerts[i] - 1);
            Float[] worldVert = points.get(faceVerts[i] - 1);

            colorAvg += (worldVert[1] - bounds[0]) / bounds[1];

            if(vert[0] == null) {
                continue OUTERLOOP;
            }

            origin[0] += worldVert[0];
            origin[1] += worldVert[1];
            origin[2] += worldVert[2];

            faceXPoints[i] = Math.round(vert[0]);
            faceYPoints[i] = Math.round(vert[1]);
            // centerY += vert[1];
        }

        origin[0] /= length;
        origin[1] /= length;
        origin[2] /= length;

        Float distance = getDistanceToPlayer(origin);

        int rgb = Math.min(Math.round((colorAvg / length) * 150 + 50), 150);

        // if (sizeFDR == 0) {
        //     faceRenderOrder.add(new Polygon(faceXPoints, faceYPoints, length));
        //     faceDistances.add(distance);
        //     sizeFDR++;

        //     colors.add(rgb);
        // } else {
        //     for(int d = 0; d < sizeFDR; d++) {
        //         if (distance <= faceDistances.get(d)) {
        //             int fdIndex = d == 0 ? 0 : d - 1;
        //             Polygon face = new Polygon(faceXPoints, faceYPoints, length);
        //             faceRenderOrder.add(fdIndex, face);
        //             faceDistances.add(fdIndex, distance);
        //             sizeFDR++;

        //             colors.add(fdIndex, rgb);
        //         }
        //     }
        // }
        
        // centerY /= (length * screenDimensions[1]);
        // centerY = Math.min((Math.abs(centerY)), 1) * 254;

        // int rgb = Math.min(Math.round((colorAvg / length) * 150 + 50), 150);

        // g.setColor(new Color(0, 0, 0));
        // g.drawPolygon(faceXPoints, faceYPoints, length);
        // g.setColor(new Color(rgb, rgb, rgb));
        // g.fillPolygon(faceXPoints, faceYPoints, length);
    }
}
// for (int f = 0; f < sizeFDR; f++) {
//     // g.setColor(colors.get(f));
//     g.fillPolygon(faceRenderOrder.get(f));
// }
