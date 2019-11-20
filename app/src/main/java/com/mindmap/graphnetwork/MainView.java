package com.mindmap.graphnetwork;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Iterator;

enum ViewTask{
    MOVE_NODE,EDIT_NODE,SHOW_CONNECTED,
    MOVE_EDGE,EDIT_EDGE,
    SWIPE_SCREEN,ZOOM_SCREEN,
    IDLE;
}
enum DrawableType{
   NODE,EDGE;
}

public class MainView extends View implements View.OnClickListener,View.OnLongClickListener {
    //Items: everything here needs to be saved/loaded
    private ArrayList<MindMapDrawable> mAllViewDrawables;
    private Context mContext = null;
    private MindMapDrawable mSelected = null; //null means none are selected
    private MindMapDrawable mClicked = null; //null means none are selected
    private MindMapDrawable mLongClicked = null;
    //constant for defining the maximum total time duration between the first click and second click that can be considered as double-click
    private static final long MAX_DOUBLE_CLICK_DURATION = 500;
    //variable for counting two successive up-down events
    int mClickCount = 0;
    Edge mEdge = null;
    ViewTask mViewTask;
    private float mDownX,mDownY;
    private long mClickEnd = 0;
    PopupWindow mPopupWindow;

    //coordinates to shift the Drawables by
    public float mSwipeDownX=0f;
    public float mSwipeDownY=0f;
    public float mShiftX=0f;
    public float mShiftY=0f;

    ScaleGestureDetector mScaleDetector;
    float mMinScale = 0.25f;
    float mMaxScale = 4f;
    float mScaleFactor = 1f;

    void zoom(float scale){
        mViewTask = ViewTask.ZOOM_SCREEN;
        mScaleFactor *= scale;
        mScaleFactor = Math.max(mMinScale,Math.min(mMaxScale,mScaleFactor));
        scaleDrawables();
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            mViewTask = ViewTask.ZOOM_SCREEN;
            return true;
        }
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(mMinScale,Math.min(mMaxScale,mScaleFactor));
            scaleDrawables();
            return true;
        }
    }


    public MainView(Context context) {
        super(context);
        init(context);
    }

    public MainView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context,attrs,defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MainView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context,attrs,defStyleAttr,defStyleRes);
        init(context);
    }

    void init(Context context){
        this.setClickable(true);
        this.setOnClickListener(this);
        this.setOnLongClickListener(this);
        mAllViewDrawables = new ArrayList<>();
        mContext = context;
        mViewTask = ViewTask.IDLE;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    /**
     * implementation things to draw
     * @param canvas to draw items
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        paint.setColor( Color.RED );
//        canvas.drawCircle(-100,-100,500,paint);
        for (MindMapDrawable drawable : mAllViewDrawables) {
            if (drawable.onScreen( getWidth(), getHeight() ))
                    drawable.draw(canvas);
        }

    }

    public void addDrawable(MindMapDrawable drawable) {
        mAllViewDrawables.add(drawable);
        postInvalidate();
    }

    public void addNode(float x,float y){
//        Node node = new Node(mContext);
//        node.initNode(x,y,this);
        Node node = new Node(x,y,this);
        addDrawable(node);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        //mScaleDetector.onTouchEvent(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mViewTask = ViewTask.SWIPE_SCREEN;//take precedence over all other actions
                mSwipeDownX = event.getX(0);//take first finger as reference by default getX() is getX(0)
                mSwipeDownY = event.getY(0);
                break;
            case MotionEvent.ACTION_DOWN:
                if(mViewTask == ViewTask.EDIT_NODE || mViewTask == ViewTask.EDIT_EDGE) {
                    mPopupWindow.dismiss();
                    mViewTask = ViewTask.IDLE;
                }
                mSelected = findItem(event.getX(), event.getY());
                mDownX = event.getX();
                mDownY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveX = event.getX();
                float moveY = event.getY();

                if(mViewTask == ViewTask.SWIPE_SCREEN){//TODO Stat moving only after we cross a threshold for detecting gesture
                    mShiftX = moveX-mSwipeDownX;
                    mShiftY = moveY-mSwipeDownY;
                    mSwipeDownX = moveX;
                    mSwipeDownY = moveY;
                    moveDrawables();
                    return true;
                }

                if(mSelected != mClicked || mSelected==null ) {
                    Toast.makeText( mContext, "nothing to move", Toast.LENGTH_SHORT ).show();
                    return true;
                }

                if(mViewTask == ViewTask.MOVE_NODE) {
                    Node selectedNode = (Node) mSelected;
                    moveNode(selectedNode,moveX,moveY);
                    postInvalidate();
                    return true;
                }
                else if(mViewTask==ViewTask.MOVE_EDGE){
                    mEdge.setEnd(moveX,moveY);
                    postInvalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mViewTask == ViewTask.SWIPE_SCREEN){
                    mViewTask = ViewTask.IDLE;
                    return true;
                }
                if(mViewTask == ViewTask.MOVE_NODE)
                    mViewTask = ViewTask.IDLE;
                if(mViewTask == ViewTask.MOVE_EDGE){
                    float upX = event.getX();
                    float upY = event.getY();
                    Node toNode =  (Node)findItem(upX, upY,DrawableType.NODE);
                    if(toNode == null){
                        if( mAllViewDrawables.size() > 0 )
                            mAllViewDrawables.remove( mAllViewDrawables.size() - 1 );
                    }
                    else {
                        mEdge.setToNode(toNode);
                        float[] centreXY = (toNode).centre();
                        mEdge.setEnd(centreXY[0],centreXY[1]);
                        mEdge.MakeEditable( false );
                    }
                    mEdge = null;
                    mViewTask = ViewTask.IDLE;
                    postInvalidate();
                    return true;
                }
                break;
        }

//        postInvalidate();
        return true;
    }


    @Override
    public void onClick(View view){
        //Double CLick
        if(mClicked!=null){
           MindMapDrawable  pClicked = mClicked;
            //search in only correct viewable type
           mClicked = findItem(mDownX, mDownY,pClicked.type());
           if(pClicked == mClicked && System.currentTimeMillis()-mClickEnd < MAX_DOUBLE_CLICK_DURATION){
               mClickCount = 2;
               mClickEnd = System.currentTimeMillis();
               if(mClicked instanceof Node) {
                   if(mViewTask == ViewTask.IDLE) {
                       float[] centreXY = ((Node)mClicked).centre();
                       mEdge = new Edge( centreXY[0], centreXY[1], centreXY[0], centreXY[1],this );
                       mEdge.setFromNode((Node)mClicked);
                       mAllViewDrawables.add( mEdge );
                       mViewTask = ViewTask.MOVE_EDGE;
                       postInvalidate();
                   }
               }
//               Toast.makeText( mContext, "Double Clicked  node", Toast.LENGTH_SHORT ).show();
               return;
           }
        }

        //Single Click
        //search for node first
        mClicked = findItem(mDownX, mDownY,DrawableType.NODE);
        if(mClicked==null)
            mClicked = findItem(mDownX, mDownY,DrawableType.EDGE);
        if(mClicked!=null) {
//            Toast.makeText( mContext, "Single Clicked  node", Toast.LENGTH_SHORT ).show();

            if(mClicked instanceof Node) {
                if( mViewTask == ViewTask.IDLE)
                    mViewTask = ViewTask.MOVE_NODE;
            }

            mClickCount = 1;
            mClickEnd = System.currentTimeMillis();
            return;
        }
        if(mViewTask != ViewTask.MOVE_NODE && mViewTask != ViewTask.MOVE_EDGE)
            mViewTask = ViewTask.IDLE;
        return;
    }


    @Override
    public boolean onLongClick(View view) {
        mLongClicked = findItem(mDownX, mDownY,DrawableType.NODE);
        if(mLongClicked==null)
            mLongClicked = findItem(mDownX, mDownY,DrawableType.EDGE);

        if(mLongClicked!=null) {
//            Toast.makeText( mContext, "Long Clicked", Toast.LENGTH_SHORT ).show();
            if(mLongClicked instanceof Node) {
                if( mViewTask == ViewTask.IDLE) {
                    mViewTask = ViewTask.EDIT_NODE;

                    LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View NodePopupLayout = layoutInflater.inflate(R.layout.node_popup_window,null);
                    showPopUp(NodePopupLayout);

                    Button DeleteNodeButton = NodePopupLayout.findViewById(R.id.DeleteNode);
                    DeleteNodeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteItem(mLongClicked);
                            mPopupWindow.dismiss();
                            mViewTask = ViewTask.IDLE;
                        }
                    });
                    Button OpenNodeButton = NodePopupLayout.findViewById(R.id.OpenNode);
                    OpenNodeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //Open Node
                            mPopupWindow.dismiss();
                            mViewTask = ViewTask.IDLE;
                        }
                    });
                }
            }
            if(mLongClicked instanceof Edge) {
                if( mViewTask == ViewTask.IDLE) {
                    mViewTask = ViewTask.EDIT_EDGE;
                    LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View NodePopupLayout = layoutInflater.inflate(R.layout.edge_popup_window,null);
                    showPopUp(NodePopupLayout);

                    Button DeleteEdgeButton = NodePopupLayout.findViewById(R.id.DeleteEdge);
                    DeleteEdgeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteItem(mLongClicked);
                            mPopupWindow.dismiss();
                            mViewTask = ViewTask.IDLE;
                        }
                    });

                }
            }
        }
        return true;
    }

    public void showPopUp(View view){
        if(mPopupWindow!=null)
            mPopupWindow.dismiss();
        mPopupWindow= new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mPopupWindow.showAtLocation(this, Gravity.NO_GRAVITY, (int)mDownX, (int)mDownY);
    }


    public MindMapDrawable findItem(float x, float y){
        return findItem(x,y,null);
    }

    public MindMapDrawable findItem(float x, float y, DrawableType type) {
        for (MindMapDrawable drawable : mAllViewDrawables)
            if (drawable.contains( (int)(x), (int)(y) ) && ((type == null)||(type == drawable.type())))
                return drawable;
        return null;
    }

    public void deleteItem(MindMapDrawable item) {
        if (item != null) {
            if (item instanceof Node) {
                Iterator<MindMapDrawable> iterator = mAllViewDrawables.iterator();
                //check all the items and see if there's an arrow pointing to the item we will remove
                while (iterator.hasNext()) {
                    MindMapDrawable drawable = iterator.next();
                    if (drawable instanceof Edge) {
                        Edge edge = (Edge) drawable;
                        if (edge.fromNode((Node)item)||edge.toNode((Node)item))
                            iterator.remove();
                    }
                }
            }
            mAllViewDrawables.remove(item);
        }
        postInvalidate();
    }

    public void moveNode(Node node,float moveX,float moveY) {
        Iterator<MindMapDrawable> iterator = mAllViewDrawables.iterator();
        while (iterator.hasNext()) {
            MindMapDrawable drawable = iterator.next();
            if (drawable instanceof Edge) {
                Edge edge = (Edge) drawable;
                if (edge.fromNode(node))
                    edge.setStart(moveX,moveY);
                if (edge.toNode(node))
                    edge.setEnd(moveX,moveY);
            }
        }
        node.set( moveX, moveY );
    }

    public void moveDrawables(){
        for (MindMapDrawable drawable : mAllViewDrawables) {
            drawable.move(mShiftX,mShiftY);
        }
        postInvalidate();
    }

    public void scaleDrawables(){
        for (MindMapDrawable drawable : mAllViewDrawables) {
            drawable.scale(mScaleFactor);
        }
        postInvalidate();
    }


}
