package me.skonb.openh264cameraview

import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Track
import java.io.File
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import java.io.RandomAccessFile
import java.util.*


/**
 * Created by skonb on 2017/02/20.
 */
class MP4Concatenator {
    companion object {
        fun concat(files: List<File>, output: File) {
            val movies = LinkedList<Movie>()
            files.forEach { file ->
                movies.add(MovieCreator.build(file.absolutePath))
            }

            val videoTracks = LinkedList<Track>()
            val audioTracks = LinkedList<Track>()


            for (m in movies) {
                for (track in m.tracks) {
                    if (track.handler == "vide") {
                        videoTracks.add(track)
                    }
                    if (track.handler == "soun") {
                        audioTracks.add(track)
                    }
                }
            }

            val concatMovie = Movie()

            concatMovie.addTrack(AppendTrack(*videoTracks.toTypedArray()))
            concatMovie.addTrack(AppendTrack(*audioTracks.toTypedArray()))


            val out2 = DefaultMp4Builder().build(concatMovie)
            val fc = RandomAccessFile(output, "rw").channel
            fc.position(0)
            out2.writeContainer(fc)
            fc.close()
        }
    }
}
