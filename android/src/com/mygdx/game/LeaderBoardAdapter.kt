import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.mygdx.game.flappydino.R

class LeaderboardAdapter(private val context: Context, private val data: List<Pair<String, Int>>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val customFont: Typeface = Typeface.createFromAsset(context.assets, "myfont.ttf")

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Any = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.leaderboard_list_item, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as ViewHolder
        }

        val (username, score) = data[position]
        holder.usernameTextView.text = username
        holder.scoreTextView.text = score.toString()

        // 커스텀 폰트 설정
        holder.usernameTextView.typeface = customFont
        holder.scoreTextView.typeface = customFont

        return view
    }

    private class ViewHolder(view: View) {
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val scoreTextView: TextView = view.findViewById(R.id.scoreTextView)
    }
}
