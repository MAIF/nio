package utils

import java.util.concurrent.atomic.AtomicReference

import de.flapdoodle.embed.mongo.config.{
  DownloadConfigBuilder,
  MongodConfigBuilder,
  Net,
  RuntimeConfigBuilder
}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{
  Command,
  MongodExecutable,
  MongodProcess,
  MongodStarter
}
import de.flapdoodle.embed.process.config.io.ProcessOutput
import de.flapdoodle.embed.process.extract.UserTempNaming
import de.flapdoodle.embed.process.io.directories.TempDirInPlatformTempDir
import de.flapdoodle.embed.process.runtime.ICommandLinePostProcessor
import de.flapdoodle.embed.process.store.{ArtifactStoreBuilder, Downloader}

trait WithMongo {

  val mongoProcess: AtomicReference[MongodProcess] =
    new AtomicReference[MongodProcess]()

  val mongoPort = Tools.nextFreePort

  def startMongo() = {
    val mongodExecutable: MongodExecutable = MongodStarter
      .getInstance(
        new RuntimeConfigBuilder()
          .daemonProcess(true)
          .artifactStore(
            new ArtifactStoreBuilder()
              .executableNaming(new UserTempNaming())
              .tempDir(new TempDirInPlatformTempDir())
              .download(
                new DownloadConfigBuilder()
                  .defaultsForCommand(Command.MongoD)
                  .defaults()
                  .build()
              )
              .downloader(new Downloader())
              .build()
          )
          .processOutput(ProcessOutput.getDefaultInstanceSilent)
          .commandLinePostProcessor(new ICommandLinePostProcessor.Noop)
          .build()
      )
      .prepare(
        new MongodConfigBuilder()
          .version(Version.V3_4_1)
          .net(new Net(this.mongoPort, false))
          .build()
      )

    this.mongoProcess.set(mongodExecutable.start())
  }

  def stopMongo() = {
    this.mongoProcess.get().stop()
  }

  def getMongoPort(): String = {
    String.valueOf(mongoPort)
  }

}
