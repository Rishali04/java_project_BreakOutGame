import java.awt.*;
import java.awt.event.*;
import java.applet.*;


/*<applet code="BreakOutGame" height="930" width="1850"></applet>*/

public class BreakOutGame extends Applet implements Runnable,KeyListener,FocusListener 
{

  static final int PADDLE_WIDTH=100;
  static final int PADDLE_HEIGHT=10;
  static final int PADDLE_STEP=8;

  static final int BALL_DIAMETER=30;
  static final int BALL_STEP=5;

  // A second Screen for Buffering (Double Buffer Technique)

   Image OffScreenImg;
  Graphics OffScreenGraphics;  

  int Width,Height;

  Brick_Wall Brick_Wall;
  Paddle Paddle;
  Ball Ball;

   boolean Running,Suspend,gameOver,Waiting_For_Space;

  volatile boolean Right_Pressed,Left_Pressed,Space_Was_Pressed;

   //_______________________________________________________________________________________________________________________

  // Waiting For the Space to be pressed ..........

  void WaitForSpace() {
    Waiting_For_Space=true;
    repaint();

    // In sleep - Resources are less consumed ......

    Space_Was_Pressed=false;
    while (!Space_Was_Pressed) {
      try {
        Thread.currentThread().sleep(100);
      }
      catch (InterruptedException e) {};
    }
    Waiting_For_Space=false;
  }

   // ____________________________________________________________________________________________________________________

  // Part of FocusListener 

  // Game will be Start or Resumed , If the user clicks on applet....................

   public void focusGained(FocusEvent f) {
    if (!Running) {
      Suspend=false;
      Running=true;
      (new Thread(BreakOutGame.this)).start();
    } else {
      Suspend=false;
    }
    repaint();
  } 

   // Game will get suspended , If the user clicks somewhere else....................

   public void focusLost(FocusEvent f) {
    if (Running) {
      Suspend=true;
      repaint();
    }
  }

   //______________________________________________________________________________________________________________________

  // Part of KeyListener
  
  public void keyPressed(KeyEvent k) {
    switch (k.getKeyCode()) {
      case KeyEvent.VK_LEFT:  Left_Pressed=true;
                                 break;
      case KeyEvent.VK_RIGHT: Right_Pressed=true;
                                 break;
      case KeyEvent.VK_SPACE: Space_Was_Pressed=true;
                                 break;
    }
  }

  public void keyReleased(KeyEvent k) {
    switch (k.getKeyCode()) {
      case KeyEvent.VK_LEFT:  Left_Pressed=false;
                                 break;
      case KeyEvent.VK_RIGHT: Right_Pressed=false;
                                 break;
    }
  }
   public void keyTyped(KeyEvent k)
   {

   }

   //_______________________________________________________________________________________________________________________


    public void init() 
  {   
    setBackground(Color.pink);                        //Background for Welcome Screen............

    Running=false;
    Width=getSize().width;
    Height=getSize().height;

    OffScreenImg = createImage(Width,Height);
    OffScreenGraphics = OffScreenImg.getGraphics();

    addKeyListener(this);    
    addFocusListener(this);

    Waiting_For_Space=false;

    repaint();
  }

   public void paint(Graphics g) 
   {   
    if (Running) 
    {
      g.drawImage(OffScreenImg, 0, 0, this);    
      if (Suspend) 
      {
        g.setColor(Color.white);
        g.drawString("Click here.",(Width-70)/2,Height/2);
      } 
      else if (Waiting_For_Space)
      {
        g.setColor(Color.white);
        g.drawString("Press SPACE.",(Width-70)/2,Height/2);
      }

    } 
    else 
      { 
        Font f1 = new Font("Comic San MS",Font.ITALIC,24);
        g.setFont(f1);
        g.setColor(Color.black); 
        g.drawString("Click here to start.",(Width-90)/2,Height/2);
      }
    }


 public void update(Graphics g) 
 {
    paint(g);
  }

  public void run() 
  {
    while (true) 
    {
      OffScreenGraphics.setColor(Color.black);
      OffScreenGraphics.fillRect(0,0,Width,Height);

      gameOver=false;

      Brick_Wall=new Brick_Wall(10,4,Width/10,Height/(3*4),OffScreenGraphics);
      Brick_Wall.paint();
      Paddle=new Paddle(PADDLE_WIDTH,PADDLE_HEIGHT,(Width-PADDLE_WIDTH)/2,Height-1-PADDLE_HEIGHT,0,Width,PADDLE_STEP,OffScreenGraphics);
      Paddle.paint();
      Ball=new Ball(Width/2,Height/3,0,BALL_STEP,BALL_DIAMETER,BALL_STEP,OffScreenGraphics,0,Width,0,Height);
      Ball.paint();
   
      repaint();

       WaitForSpace();
        
      while (!gameOver) 
      {
        try 
        {
          Thread.currentThread().sleep(10);
        }
        catch (InterruptedException e) 
        {

        };

        if (!Suspend) 
        {
          Paddle.clear(); 
          Ball.clear();

          if ((Left_Pressed)&&(!Right_Pressed))
          {
            Paddle.Move_Left();
          }
          if ((Right_Pressed)&&(!Left_Pressed)) 
          {
            Paddle.Move_Right();
          }

          gameOver=Ball.move(Paddle,Brick_Wall);

          if (Brick_Wall.bricks_Left()==0) 
           {
             gameOver=true;
           }

          Paddle.paint();
          Ball.paint();
     
          repaint();
        }
      }

      class Score
{ 
  int i=0;
  public void paint(Graphics g)
  {
      g.setColor(Color.yellow);
      g.drawString("SCORE : "+ i +"",750,1700);
      i+=50;
    
  }
}


       OffScreenGraphics.setColor(Color.white);

      if (Brick_Wall.bricks_Left()==0) 
      {
        OffScreenGraphics.drawString("CONGRATULATIONS!",(Width-120)/2,Height/2-20); 
      }
      else 
      {
        OffScreenGraphics.drawString("GAME OVER!",(Width-76)/2,Height/2-20); 
      }

      WaitForSpace();
    }
  }
}

/*_________________________________________________________________________________________________________________________

          class Brick_Wall       - will manage the bricks in the BreakOut game
   
   METHODS: 
           void paint()          - will paints all the bricks in game
     int in_Brick(int x,int y)   - will returns 1 if the point x,y is inside some brick
     void Hit_Brick(int x,int y) - will deletes the brick which contains the point (x,y)
         int bricks_Left()       - will returns the number of bricks left
___________________________________________________________________________________________________________________________*/

class Brick_Wall 
{
  private boolean Brick_Visible[][];
  private int bricks_In_Row,bricks_In_Column,brick_Width,brick_Height,bricks_Left;
  Graphics g;

  public Brick_Wall(int bricks_In_Row_,int bricks_In_Column_,int brick_Width_,int brick_Height_,Graphics g_) 
  {
    bricks_In_Row=bricks_In_Row_;
    bricks_In_Column=bricks_In_Column_;
    brick_Width=brick_Width_;
    brick_Height=brick_Height_;
    g=g_;

    Brick_Visible=new boolean[bricks_In_Row][bricks_In_Column];
    bricks_Left=0;

    int x,y;
    for (x=0;x<bricks_In_Row;x++)
      for (y=0;y<bricks_In_Column;y++) 
      {
        Brick_Visible[x][y]=true;
        bricks_Left++;
      }
  }

    public void paint() 
    {
   		 int x,y;

   		 for (x=0;x<bricks_In_Row;x++)
	     for (y=0;y<bricks_In_Column;y++)
        if (Brick_Visible[x][y]) 
        {
          g.setColor(Color.blue);
          g.fillRect(x*brick_Width,y*brick_Height,brick_Width-1,brick_Height-1);
        }
  	}

  	public int in_Brick(int x,int y)
   {
    int mx,my;
 
    mx=(x/brick_Width);
    my=(y/brick_Height);

    if ((mx<0)||(mx>=bricks_In_Row)||(my<0)||(my>=bricks_In_Column))
    {
     return 0;
    }

    if (Brick_Visible[mx][my])
     {
        return 1;
     }
      else
      {
       return 0;
      }
  }

   public void Hit_Brick(int x,int y) 
   {

    int mx,my;
    int Score=0;

    mx=(x/brick_Width);
    my=(y/brick_Height);

    if ((mx<0)||(mx>=bricks_In_Row)||(my<0)||(my>=bricks_In_Column)) 
    {
      return;
    }
     
    if (Brick_Visible[mx][my]) 
    {
      Brick_Visible[mx][my]=false;
      bricks_Left--;
      g.setColor(Color.black);
      g.fillRect(mx*brick_Width,my*brick_Height,brick_Width-1,brick_Height-1);
  
    }
  }   

   public int bricks_Left() 
  {
    return bricks_Left;
  }
}   

/*_________________________________________________________________________________________________________________________

   class Paddle - this will manage the paddle in the Breakout Game
   
   methods: 
     void paint()      - will paints the Paddle
     void clear()      - will clears the Paddle
     void Move_Left()  - will moves the Paddle to the Left
     void Move_Right() - will moves the Paddle to the Right
__________________________________________________________________________________________________________________________*/

class Paddle 
{
  private int Width,Height,x,y,Maxx,Minx,Step;
  private Graphics g;

  public Paddle(int width_,int height_,int x_,int y_,int minx_,int maxx_,int step_,Graphics g_) 
  {
    Width=width_;
    Height=height_;
    x=x_; y=y_;
    Minx=minx_;
    Maxx=maxx_;
    g=g_;
    Step=step_;
  } 

  public void paint() 
  {
    g.setColor(Color.white);
    g.fillRect(x,y,Width,Height);
  }

  public void clear() 
  {
    g.setColor(Color.black);
    g.fillRect(x,y,Width,Height);
  }

  public void Move_Left() 
  {
    if(x-Step>Minx)
    {
     x-=Step; 
    }
    else
     {
      x=Minx;
     }
  }

  public void Move_Right() 
  {
    if (x+Step<Maxx-Width) 
    {
      x+=Step; 
    }
    else
    { 
      x=Maxx-Width;
    }
  }

 public int Left_Corner() 
  {
    return x;
  }

  public int Right_Corner()
  {
    return x+Width;
  }
  
  public int Middle() 
  {
    return x+Width/2;
  }

  public int GetY() 
  {
    return y;
  }
}

/*__________________________________________________________________________________________________________________________

   class Ball - will manage the Ball in the BreakOut Game
   
   METHODS : 
     void paint() - will paints the ball
     void clear() - will clears the ball
     boolean move(Paddle Paddle,Brick_Wall Brick_Wall) - will moves the ball & returns true 
                                                       if the ball goes off the screen

___________________________________________________________________________________________________________________________*/

class Ball
{
  private int x,y,dx,dy,Diameter,Minx,Maxx,Miny,Maxy,Step;
  private Graphics g;

  public Ball(int x_,int y_,int dx_,int dy_,int diameter_,int step_,Graphics g_,int minx_,int maxx_,int miny_,int maxy_) 
  {
    x=x_; 
    y=y_; 
    dx=dx_; 
    dy=dy_; 
    Diameter=diameter_; 
    Step=step_; 
    g=g_;
    Minx=minx_; 
    Maxx=maxx_; 
    Miny=miny_; 
    Maxy=maxy_;
  }

  public void paint() 
  {
    g.setColor(Color.white);
    g.fillOval(x,y,Diameter,Diameter);
  }
  
  public void clear() 
  {
    g.setColor(Color.black);
    g.fillOval(x,y,Diameter,Diameter);
  }

  public boolean move(Paddle Paddle,Brick_Wall Brick_Wall) {
    boolean BallGoesOut=false;

    // If ball hit the wall --> bounce in favourable direction
    if ((x+dx<Minx)||(x+dx+Diameter>Maxx)) 
    {
      dx=-dx;
    }
    if (y+dy<0) 
    {
      dy=-dy;
    }
  
    if (y+dy+Diameter>=Paddle.GetY()) 
    {
      if ((x+dx+Diameter<Paddle.Left_Corner())||(x+dx>Paddle.Right_Corner()))
      {
         BallGoesOut=true;
      }
       else 
       {
         dy=-dy;
         if (x+dx+Diameter/2<Paddle.Middle()) 
          {
            dx=-Step;
          }
         else
         {
           dx=Step;
         }
       }
    }

     switch (Brick_Wall.in_Brick(x,y)+2*Brick_Wall.in_Brick(x+Diameter,y)+4*Brick_Wall.in_Brick(x,y+Diameter)+8*Brick_Wall.in_Brick(x+Diameter,y+Diameter)) 
    {
      case 0: break;
      case 1: dx=Step; dy=Step;
              break;
      case 2: dx=-Step; dy=Step;
              break;
      case 3: case 12: dy=-dy; 
              break;
      case 4: dx=Step; dy=-Step; 
              break;
      case 5: case 10: dx=-dx; 
              break;
      case 8: dx=-Step; dy=-Step; 
              break;
      default: dx=-dx; dy=-dy; 
              break;
    }
           
    Brick_Wall.Hit_Brick(x,y); 
    Brick_Wall.Hit_Brick(x+Diameter,y); 
    Brick_Wall.Hit_Brick(x,y+Diameter); 
    Brick_Wall.Hit_Brick(x+Diameter,y+Diameter);
   
    x+=dx;
    y+=dy;
               
    return BallGoesOut;
  }
}


//______________________________________________________________________________________---

class Score
{ 
  int i=0;
  public void paint(Graphics g)
  {
      g.setColor(Color.yellow);
      g.drawString("SCORE : "+ i +"",750,1700);
      i+=50;
    
  }
}

  


