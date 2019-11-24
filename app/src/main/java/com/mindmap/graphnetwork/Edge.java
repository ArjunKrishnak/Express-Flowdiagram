package com.mindmap.graphnetwork;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;

import org.json.JSONObject;

import java.util.List;

enum ArrowShape { START,END,DOUBLE,NONE };

public class Edge implements MindMapDrawable{

    private static final String TAG = "Edge";

    //Saved in Json
    private String mId;
    private int mEdgeColorID;
    private float mEdgeStrokeWidth;
    private Node mFromNode;
    private Node mToNode;
    private String mTitle = "";
    private String mDescription  = "";
    private ArrowShape mArrowShape = ArrowShape.NONE;
    //Not Saved in Json
    private static final int DEFAULT_STROKE_WIDTH = 12,DEFAULT_EDGE_COLOR = Color.RED;
    private static final float DEFAULT_CURSOR_RADIUS = 30, DEFAULT_CURSOR_STROKE_WIDTH = 4f;
    private static final int DEFAULT_TITLE_COLOR = Color.BLUE;
    private Path mPath,mStartCursorPath,mEndCursorPath;
    private Paint mPaint,mCursorPaint,mTitlePaint;
    //Edge state variables
    private boolean mEditable;
    private MainView mParentView;
    private float mCurrentScale = 1f;
    private float mStartX,mStartY,mEndX,mEndY;

    public void setArrowShape(ArrowShape arrowShape){
        mArrowShape = arrowShape;
    }

    public ArrowShape getArrowShape() {
        return mArrowShape;
    }

    public PointF getStartXY() {
        return new PointF( mStartX, mStartY );
    }

    @Override
    public String getTitle(){
        return mTitle;
    }

    @Override
    public String getDescription(){
        if(mDescription==null)
            return "";
        return mDescription;
    }

    @Override
    public void setTitle(String title){
        mTitle = title;
//        Rect boundTitle = new Rect()
//        mTitlePaint.getTextBounds(title, 0, title.length(), boundTitle);
//        //Add ... if title size exceeds the length of edge
//        float length = (float)Math.sqrt((mStartX-mEndX)*(mStartX-mEndX) + (mStartY-mEndY)*(mStartY-mEndY));
//        if(length<boundTitle.width()) {
//            mR = boundTitle.width() / 2;
        }

    @Override
    public void setDescription(String description){
        if(description==null)
            mDescription = "";
        else
            mDescription = description;
    }

    @Override
    public void setColorID(int colorID) {
        this.mEdgeColorID = colorID;
    }

    @Override
    public int getColorID() {
        return mEdgeColorID;
    }

    public DrawableType type(){
        return DrawableType.EDGE;
    }

    //Called from MainView
    Edge(float startX,float startY,float endX,float endY,MainView parent){
        setStart(startX,startY);
        setEnd(endX,endY);
        init(parent);
        editable(true);
        mId = JsonHelper.getUniqueID();
    }

    //Called while decoding Json
    Edge(Node fromNode,Node toNode,String Id,MainView parent,int colorID,String title,String description,ArrowShape arrowShape){
        float[] startXY = (fromNode).centre();
        float[] endXY = (toNode).centre();
        setStart(startXY[0],startXY[1]);
        setEnd(endXY[0],endXY[1]);
        setFromNode(fromNode);
        setToNode(toNode);
        init( parent );
        editable(false);
        mId = Id;
        setColorID( colorID );
        setTitle( title );
        setDescription( description);
        setArrowShape(arrowShape);
    }

    public void setFromNode(Node fromNode){
        mFromNode = fromNode;
    }
    public void setToNode(Node toNode){
        mToNode = toNode;
    }

    public void setStart(float startX,float startY) {
        mStartX = startX;
        mStartY = startY;
    }
    public void setEnd(float endX,float endY) {
        mEndX = endX;
        mEndY = endY;
    }

    public void editable(boolean state){
        mEditable = state;
    }

    private void init(MainView parent){
        mParentView = parent;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath = new Path();
        mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setStrokeJoin(Paint.Join.MITER);
        mCursorPaint.setStrokeWidth( DEFAULT_CURSOR_STROKE_WIDTH );
        mStartCursorPath = new Path();
        mEndCursorPath = new Path();
        mEdgeColorID = DEFAULT_EDGE_COLOR;
        mPaint.setColor( mEdgeColorID );
        mCurrentScale = mParentView.mScaleFactor;
        mEdgeStrokeWidth = (int)(DEFAULT_STROKE_WIDTH*mCurrentScale);
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
        mCursorPaint.setColor( mEdgeColorID );
        mTitlePaint= new Paint();
        mTitlePaint.setColor( DEFAULT_TITLE_COLOR );
        mTitlePaint.setTextSize(30);
        mTitlePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void draw(Canvas canvas) {
        mPath.reset();
        mPath.moveTo(mStartX, mStartY);
        mPath.quadTo(mStartX, mStartY, mEndX,mEndY);
        mPaint.setColor( mEdgeColorID );
        canvas.drawPath( mPath,  mPaint);

        //for maintaing orientation of the text.
        if(mStartX>mEndX){
            mPath.reset();
            mPath.moveTo(mEndX, mEndY);
            mPath.quadTo(mEndX, mEndY, mStartX,mStartY);
        }
        canvas.drawTextOnPath(mTitle,mPath,0,3*mEdgeStrokeWidth,mTitlePaint );

        mCursorPaint.setColor( mEdgeColorID );
        if(mEditable) {
            mStartCursorPath.reset();
            mStartCursorPath.addCircle( mStartX, mStartY, DEFAULT_CURSOR_RADIUS, Path.Direction.CW );
            canvas.drawPath( mStartCursorPath, mCursorPaint );

            mEndCursorPath.reset();
            mEndCursorPath.addCircle( mEndX, mEndY, DEFAULT_CURSOR_RADIUS, Path.Direction.CW );
            canvas.drawPath( mEndCursorPath, mCursorPaint );
        }

        //drawing arrows
        if(mArrowShape == ArrowShape.NONE)
            return;

        PointF startDirectionPoint = new PointF(0,0);
        PointF endDirectionPoint = new PointF(0,0);

        if(mStartX==mEndX) {
            startDirectionPoint.x = mStartX;
            endDirectionPoint.x = mEndX;
            if (mEndY > mStartY) {
                startDirectionPoint.y = mStartY + 10;
                endDirectionPoint.y = mEndY - 10;
            } else {
                startDirectionPoint.y = mStartY - 10;
                endDirectionPoint.y = mEndY + 10;
            }
        }
        else {
            float slope = -(mStartY - mEndY) / (mStartX - mEndX);//android cordinate increases downwards unlike ordinary coordinate
            if(mStartX<mEndX) {
                startDirectionPoint.x = mStartX + 10;
                startDirectionPoint.y = mStartY - slope * 10;
                endDirectionPoint.x = mEndX - 10;
                endDirectionPoint.y = mEndY + slope*10;
            }
            else{
                startDirectionPoint.x = mStartX - 10;
                startDirectionPoint.y = mStartY + slope * 10;
                endDirectionPoint.x = mEndX + 10;
                endDirectionPoint.y = mEndY - slope*10;
            }
        }

        if(mArrowShape == ArrowShape.START|| mArrowShape == ArrowShape.DOUBLE)
            drawArrowHead(canvas,new PointF(mStartX,mStartY),startDirectionPoint);
        if(mArrowShape == ArrowShape.END|| mArrowShape == ArrowShape.DOUBLE)
            drawArrowHead(canvas,new PointF(mEndX,mEndY),endDirectionPoint);

    }

    boolean fromNode(Node node){
       return mFromNode==node;
    }
    boolean toNode(Node node){
        return mToNode==node;
    }

    public boolean contains(float x, float y){
//        if(Editable){
//            if((x-mStartX)*(x-mStartX) + (y-mStartY)*(y-mStartY) <= 5*DEFAULT_CURSOR_RADIUS*DEFAULT_CURSOR_RADIUS)
//                return true;
//            if((x-mEndX)*(x-mEndX) + (y-mEndY)*(y-mEndY) <= 5*DEFAULT_CURSOR_RADIUS*DEFAULT_CURSOR_RADIUS)
//                return true;
//            return false;
//        }

        //comparing perpendicular distance from the line
        if(mStartX==mEndX) {
            if (x - mStartX <= 5*mEdgeStrokeWidth)
                return true;
            return false;
        }
        float slope = (mEndY - mStartY)/(mEndX - mStartX);
        if(Math.abs((slope*x - y + mStartY-slope*mStartX)/Math.sqrt(1+slope*slope))<=5*mEdgeStrokeWidth)
            return true;
        return false;
    }

    @Override
    public boolean onScreen(float width, float height) {

        float left,top,right,bottom;
        if(mStartX<mEndX) {
            left = mStartX;
            right = mEndX;
        }
        else {
            left = mEndX;
            right = mStartX;
        }
        if(mStartY<mEndY) {
            top = mStartY;
            bottom = mEndY;
        }
        else {
            top = mEndY;
            bottom = mStartY;
        }
        RectF boundPath = new RectF(left-mEdgeStrokeWidth,top-mEdgeStrokeWidth,right+mEdgeStrokeWidth,bottom+mEdgeStrokeWidth);
        RectF boundView = new RectF(0,0,width,height);
        return boundPath.intersect(boundView);

    }

    @Override
    public void move(float shiftX, float shiftY) {
        mStartX = mStartX+shiftX;
        mEndX = mEndX+shiftX;
        mStartY = mStartY+ shiftY;
        mEndY = mEndY+shiftY;
    }

    @Override
    public void scale(float scale) {
        float scaleFactor = scale/mCurrentScale;
        mStartX*=scaleFactor;
        mEndX*=scaleFactor;
        mStartY*=scaleFactor;
        mEndY*=scaleFactor;
        mCurrentScale = scale;
        mEdgeStrokeWidth = mEdgeStrokeWidth*scaleFactor;
        mPaint.setStrokeWidth(mEdgeStrokeWidth);
    }

    @Override
    public String getId(){
        return mId;
    }

    /**
     * Json representation of this Edge
     * @return a JSONObject containing all the information needed to save and load
     */

    @Override
    public JSONObject toJson() {
        try {
            JSONObject obj = new JSONObject();
            obj.put( JsonHelper.ITEM_TYPE_KEY, getClass().getName());
            obj.put( JsonHelper.EdgeSchema.EDGE_START_NODE_KEY, this.mFromNode.getId());
            obj.put( JsonHelper.EdgeSchema.EDGE_END_NODE_KEY, this.mToNode.getId());
            obj.put( JsonHelper.EdgeSchema.EDGE_STROKE_WIDTH_KEY, this.mEdgeStrokeWidth);
            obj.put( JsonHelper.ITEM_ID_KEY, this.getId());
            obj.put( JsonHelper.EdgeSchema.EDGE_TITLE_KEY, this.getTitle());
            obj.put( JsonHelper.EdgeSchema.EDGE_DESCRIPTION_KEY, this.getDescription());
            obj.put( JsonHelper.EdgeSchema.EDGE_COLOR_KEY, this.getColorID());
            obj.put( JsonHelper.EdgeSchema.EDGE_ARROW_TYPE_KEY, this.getArrowShape().toString());
            return obj;

        } catch (Exception e) {
            Log.e(TAG, "toJson: ", e);
            return null;
        }
    }


    private static ArrowShape shapeStringToEnum(String string){
        if(string.equals( ArrowShape.START.toString()))
            return ArrowShape.START;
        else if(string.equals( ArrowShape.END.toString()))
            return ArrowShape.END;
        else if(string.equals( ArrowShape.DOUBLE.toString()))
            return ArrowShape.DOUBLE;
        return ArrowShape.NONE;
    }

    /**
     * Get a UmlBentArrow from a saved JSONObject representing a UmlBentArrow
     *
     * @param jsonObject     representing a UmlBentArrow
     * @param alldrDrawables all possible shapes this UmlBentArrow can point to/from
     * @return a new UmlBentArrow
     */
    public static Edge fromJson(JSONObject jsonObject, List<MindMapDrawable> alldrDrawables,MainView view) {
        try {

            Node startNode = null;
            Node endNode = null;
            String startShapeId = jsonObject.getString(JsonHelper.EdgeSchema.EDGE_START_NODE_KEY);
            String endShapeId = jsonObject.getString(JsonHelper.EdgeSchema.EDGE_END_NODE_KEY);

            for (MindMapDrawable shape : alldrDrawables) {
                if (shape instanceof Node) {
                    if (shape.getId().equals(startShapeId)) startNode = (Node) shape;
                    if (shape.getId().equals(endShapeId)) endNode = (Node) shape;
                }
            }

            //return a new arrow if we found both the items
            return (startNode == null || endNode == null) ? null
                    : new Edge(startNode, endNode,jsonObject.getString( JsonHelper.ITEM_ID_KEY),view,jsonObject.getInt( JsonHelper.EdgeSchema.EDGE_COLOR_KEY),
                    jsonObject.getString( JsonHelper.EdgeSchema.EDGE_TITLE_KEY),jsonObject.getString( JsonHelper.EdgeSchema.EDGE_DESCRIPTION_KEY),
                    shapeStringToEnum(jsonObject.getString( JsonHelper.EdgeSchema.EDGE_ARROW_TYPE_KEY )));
        } catch (Exception e) {
            Log.e(TAG, "fromJson: ", e);
            return null;
        }
    }

    //how far the user can click away from an arrow to select it
    private final double ARROW_ANGLE = Math.PI / 6d;

    private void drawArrowHead(Canvas canvas, PointF location, PointF directionPoint) {

        float dx = location.x - directionPoint.x;
        float dy = (location.y - directionPoint.y);
        double angle = Math.atan2(dy, dx);
        float arrLength = 4*mEdgeStrokeWidth;

        /* logic inspired by Cay Horstmann's Violet */
        float x1 = (float) (location.x - arrLength * Math.cos(angle + ARROW_ANGLE));
        float y1 = (float) (location.y - arrLength * Math.sin(angle + ARROW_ANGLE));
        float x2 = (float) (location.x - arrLength * Math.cos(angle - ARROW_ANGLE));
        float y2 = (float) (location.y - arrLength * Math.sin(angle - ARROW_ANGLE));

        Path outlinePath = new Path();
        outlinePath.moveTo(location.x, location.y);
        outlinePath.lineTo(x1, y1);


        outlinePath.lineTo(x2, y2);
        outlinePath.close();

        Paint defaultArrowHeadFillPaint = new Paint();
        defaultArrowHeadFillPaint.setStyle(Paint.Style.FILL);
        defaultArrowHeadFillPaint.setColor(Color.BLACK);
        canvas.drawPath(outlinePath, defaultArrowHeadFillPaint);
    }


}
