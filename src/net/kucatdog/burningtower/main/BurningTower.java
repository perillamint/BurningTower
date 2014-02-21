package net.kucatdog.burningtower.main;

import java.util.Iterator;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class BurningTower implements ApplicationListener, GameContext {

	private final int VIRTUAL_WIDTH = 768;
	private final int VIRTUAL_HEIGHT = 1280;
	// private GameObject background;
	public static final int nOfFireImages = 2;
	public static Texture[] fire = new Texture[nOfFireImages];

	public final int GRIDPIXELSIZE = 40;
	// TODO: Read it from config file

	public static boolean dragLock = false;

	private Stage stage;

	private Music bgm;

	private int levelCnt;

	public static Array<GameObject> gameObjects = new Array<GameObject>();
	private SpriteBatch batch;
	private FileHandle levelFile;
	private JsonValue levelData;

	private BurningThread burner = new BurningThread(gameObjects, this);
	private Thread burningThread = new Thread(burner);

	private InputMultiplexer inputMultiplexer;

	private OrthographicCamera cam;

	public BurningTower() {
	}

	@Override
	public void create() {
		GameObject.range = 50;
		Texture.setEnforcePotImages(false);
		cam = new OrthographicCamera(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

		levelFile = Gdx.files.internal("data/levelData/level.json");
		levelData = new JsonReader().parse(levelFile);

		System.out.println(levelData);
		batch = new SpriteBatch();

		// TODO: Make background as obj.
		// background = new GameObject();
		// background.setObjType("building", false);
		// background.setPosition(0, 0);

		stage = new Stage();
		stage.setCamera(cam);

		StoreyObject background = new StoreyObject();
		background.setBounds(60, 10, 600, 400);
		stage.addActor(background);

		background = new StoreyObject();
		background.setBounds(60, 410, 600, 400);
		stage.addActor(background);

		for (int i = 0; i < nOfFireImages; i++)
			fire[i] = new Texture(Gdx.files.internal("data/image/fire"
					+ (i + 1) + ".png"));

		bgm = Gdx.audio.newMusic(Gdx.files.internal("data/audio/fire.ogg"));

		levelCnt = levelData.get("levelCnt").asInt();

		Iterator<JsonValue> levelIterator = levelData.get("1").iterator();

		while (levelIterator.hasNext()) {
			JsonValue objects = levelIterator.next();

			final GameObject object = new GameObject();

			object.setObjType(objects.get("type").asString(),
					objects.get("leaveRuin").asBoolean());
			object.resist = objects.get("resist").asInt();
			object.setX(objects.get("locationX").asFloat());
			object.setY(objects.get("locationY").asFloat());
			object.flameCnt = objects.get("flammable").asInt();

			if (objects.get("width") != null)
				object.setWidth(objects.get("width").asInt());
			if (objects.get("height") != null)
				object.setHeight(objects.get("height").asInt());

			if (objects.get("isMovable") == null
					|| objects.get("isMovable").asBoolean()) {
				object.addListener(new DragListener() {
					float deltax;
					float deltay;
					float firstx;
					float firsty;
					float firstgrep_x;
					float firstgrep_y;
					float original_x;
					float original_y;

					@Override
					public boolean touchDown(InputEvent event, float x,
							float y, int pointer, int button) {
						System.out.println("CLICK");

						deltax = 0;
						deltay = 0;
						firstx = object.getX();
						firsty = object.getY();

						firstgrep_x = x;
						firstgrep_y = y;

						original_x = firstx;
						original_y = firsty;

						return true;
					}

					@Override
					public void touchDragged(InputEvent event, float x,
							float y, int pointer) {
						if (!BurningTower.dragLock) {
							object.setOrigin(Gdx.input.getX(), Gdx.input.getY());

							deltax = x - firstgrep_x;
							deltay = y - firstgrep_y;

							if (Math.abs(deltax) >= 40) {
								object.setX(firstx + (int) deltax
										/ GRIDPIXELSIZE * GRIDPIXELSIZE);

								firstx = object.getX();
							}

							if (Math.abs(deltay) >= 40) {

								object.setY(firsty + (int) deltay
										/ GRIDPIXELSIZE * GRIDPIXELSIZE);

								firsty = object.getY();
							}
						}
					}

					@Override
					public void touchUp(InputEvent event, float x, float y,
							int pointer, int button) {
						// Check object collapses.

						for (GameObject obj : gameObjects) {
							
							if(obj == object) continue;
							
							if (object.getX() + object.getWidth() > obj.getX()
									&& object.getX() < obj.getX()
											+ obj.getWidth()
									&& object.getY() + object.getHeight() > obj
											.getY()
									&& object.getY() < obj.getY()
											+ obj.getHeight()) {
								System.out.println("COLLIDE! with "
										+ obj.objectType);
								object.setPosition(original_x, original_y);
							}
						}
					}
				});
			}

			stage.addActor(object);
			gameObjects.add(object);
		}

		FireActor fireactor = new FireActor();
		stage.addActor(fireactor);
		
		//Pyro!
		burner.setFire(130, 10);
		final PyroActor pyro = new PyroActor(burningThread);
		
		pyro.setPosition(700, 10);
		stage.addActor(pyro);

		final Image fireButton = new Image();
		fireButton
				.setDrawable(new TextureRegionDrawable(new TextureRegion(
						new Texture(Gdx.files
								.internal("data/image/fire_button.png")))));
		fireButton.setX(0);
		fireButton.setY(0);

		fireButton.setWidth(20);
		fireButton.setHeight(20);

		fireButton.addListener(new DragListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
					int pointer, int button) {
				System.out.println("FIRE START");

				pyro.burnIt();

				return true;
			}
		});

		stage.addActor(fireButton);

		inputMultiplexer = new InputMultiplexer(stage);
		// inputMultiplexer.addProcessor(1, stage);
		Gdx.input.setInputProcessor(inputMultiplexer);

		/**** TEST CODE ****/


		// ScheduledExecutorService worker = Executors
		// .newSingleThreadScheduledExecutor();

		// worker.schedule(burningThread, 10, TimeUnit.SECONDS);
		// burningThread.start();
	}

	@Override
	public void dispose() {
		stage.dispose();
		batch.dispose();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0.2f, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());

		batch.begin();

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		cam.viewportHeight = height; // set the viewport
		cam.viewportWidth = width;
		if (VIRTUAL_WIDTH / cam.viewportWidth < VIRTUAL_HEIGHT
				/ cam.viewportHeight) {
			// sett the right zoom direct
			cam.zoom = VIRTUAL_HEIGHT / cam.viewportHeight;
		} else {
			// sett the right zoom direct
			cam.zoom = VIRTUAL_WIDTH / cam.viewportWidth;
		}
		cam.position.set(cam.zoom * cam.viewportWidth / 2.0f, cam.zoom
				* cam.viewportHeight / 2.0f, 0);
		cam.update();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void playBGM() {
		bgm.setLooping(true);
		bgm.play();
	}

	@Override
	public void stopBGM() {
		bgm.stop();
	}

	@Override
	public boolean isBGMPlaying() {
		return bgm.isPlaying();
	}
}
