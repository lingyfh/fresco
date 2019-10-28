/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.fresco.samples.showcase.imageformat.webp;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.VectorEnabledTintResources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * This fragment displays different WebP images.
 *
 * For being able to do this in your applications, you need to add the following dependencies to
 * your build.gradle file (where X.X.X matches the used Fresco version): - implementation
 * 'com.facebook.fresco:animated-webp:X.X.X' - implementation 'com.facebook.fresco:webpsupport:X.X.X'
 */
public class ImageFormatWebpListFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_webp_list, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {

    RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
    recyclerView.setItemViewCacheSize(0);

    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(new RecyclerView.Adapter<ItemHolder>() {
      @NonNull
      @Override
      public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ItemHolder(LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.webp_animated_recycler_item, viewGroup, false)) {
        };
      }

      @Override
      public void onBindViewHolder(@NonNull ItemHolder viewHolder, int i) {
        viewHolder.setData();
      }

      @Override
      public int getItemCount() {
        return 1;
      }
    });


  }

  @Override
  public int getTitleId() {
    return R.string.format_webp_title;
  }

  private static class ItemHolder extends RecyclerView.ViewHolder {

    public SimpleDraweeView avatar;
    public SimpleDraweeView tiara;

    public ItemHolder(@NonNull View itemView) {
      super(itemView);
      avatar = itemView.findViewById(R.id.avatar);
      tiara = itemView.findViewById(R.id.tiara);

      // avatar.setVisibility(View.GONE);
      tiara.setVisibility(View.GONE);
    }

    public void recycler() {

    }

    private void attachOrDetch(boolean attach) {
      if (avatar.getController() != null) {
        if (attach) {
          avatar.getController().onAttach();
        } else {
          avatar.getController().onDetach();
        }
      }
      if (tiara.getController() != null) {
        if (attach) {
          tiara.getController().onAttach();
        } else {
          tiara.getController().onDetach();
        }
      }
    }

    public void setData() {
      Uri avatarUri = Uri.parse(
          "https://zyvipres.izuiyou.com/avatars/v1/fenghuolun/animation_1571755309217475342.webp");
      ImageRequest avatarRequest = ImageRequestBuilder.newBuilderWithSource(avatarUri)
          .setResizeOptions(new ResizeOptions(150, 150))
          .build();
      DraweeController avatarController = Fresco.newDraweeControllerBuilder()
          .setUri(avatarUri)
          .setImageRequest(avatarRequest)
          .setAutoPlayAnimations(true)
          .setOldController(avatar.getController())
          .build();
      avatar.setController(avatarController);

      Uri tiaraUri = Uri
          .parse("https://zyvipres.izuiyou.com/heads/v1/star/animation_1571390393082421020.webp");
      ImageRequest tiaraRequest = ImageRequestBuilder.newBuilderWithSource(tiaraUri)
          .setResizeOptions(new ResizeOptions(150, 150))
          .build();
      DraweeController tiaraController = Fresco.newDraweeControllerBuilder()
          .setImageRequest(tiaraRequest)
          .setAutoPlayAnimations(true)
          .build();
      tiara.setController(tiaraController);
    }
  }
}
